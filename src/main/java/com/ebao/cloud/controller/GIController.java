package com.ebao.cloud.controller;

import com.ebao.cloud.common.constants.PlatFormConstant;
import com.ebao.cloud.context.ThreadContext;
import com.ebao.unicorn.proposal.restful.ProposalRestful;
import com.ebao.unicorn.quotation.restful.QuotationRestful;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/gi")
public class GIController {
    private Logger logger = LoggerFactory.getLogger(GIController.class);
   
    @Autowired
    ProposalRestful proposal;
    
    @Autowired
    QuotationRestful quotation;

    @Value("${ebao.gi.token}")
    String ebaoGIToken;
    
    
    /**
     * Typical policy issue process with create, calculate, issue etc.. 
     * 
     * @return
     */
    @GetMapping(value = "/uiCustomizedFlow")
    public String uiCustomizedFlow() {
    	Map proposalModel = getProposalModel();
    	Map output, createdProposal ;
    	String proposalNo, policyNo;
    	
    	setToken();

    	logger.info("Starting customized flow. proposalModel = " + mapToJsonString(proposalModel));
    	logger.info("Calculating premium using quotation API...");
       	output = quotation.quotation(proposalModel);
    	logger.info("DuePremium = " + output.get("DuePremium").toString());

    	logger.info("Creating proposal...");
    	output = proposal.create(proposalModel);
    	logger.info("ProposalNo=" + output.get("ProposalNo").toString());
    	proposalNo =  output.get("ProposalNo").toString();
    	createdProposal = Map.copyOf(output);
    	
    	logger.info("Calculating proposal...");
    	output = proposal.calculate(createdProposal);
       	logger.info("AdjustedPremium=" + output.get("AdjustedPremium").toString());
    	logger.info("AnnualPremium=" + output.get("AnnualPremium").toString());
    	logger.info("BeforeVatPremium=" + output.get("BeforeVatPremium").toString());
    	createdProposal = Map.copyOf(output);
    	
    	logger.info("Issuing policy...");
    	output = proposal.issuePolicyWithFullBody(createdProposal);
       	logger.info("PolicyNo=" + output.get("PolicyNo").toString());
       	logger.info("PolicyId=" + output.get("PolicyId").toString());
       	policyNo = output.get("PolicyNo").toString();
    	
       	
       	logger.info("Loading proposal...");
    	output = proposal.load(proposalNo, null, null, null);
    	logger.info(mapToJsonString(output));
    	
       	logger.info("Customized flow completed.");
    	return policyNo;	
    }
    
    @GetMapping(value = "/quotation")
    public String quotation() {
    	Map input = getProposalModel();

    	setToken();
    	Map output = quotation.quotation(input);
    	logger.info(mapToJsonString(output));
    	
    	logger.info("DuePremium=" + output.get("DuePremium").toString());

    	return output.get("DuePremium").toString();	
    }

    
    @GetMapping(value = "/calculate")
    public String calculateProposal() {
    	Map input = getProposalModel();

    	setToken();
    	Map output = proposal.calculate(input);
    	logger.info(mapToJsonString(output));
    	
    	logger.info("AdjustedPremium=" + output.get("AdjustedPremium").toString());
    	logger.info("AnnualPremium=" + output.get("AnnualPremium").toString());
    	logger.info("BeforeVatPremium=" + output.get("BeforeVatPremium").toString());
    	return output.get("AdjustedPremium").toString();
    }

    @GetMapping(value = "/create")
    public String createProposal() {
    	Map input = getProposalModel();

    	setToken();
    	Map output = proposal.create(input);
    	logger.info(mapToJsonString(output));
    	
    	logger.info("ProposalNo=" + output.get("ProposalNo").toString());
    	return output.get("ProposalNo").toString();	
    }
    

    @GetMapping(value = "/search")
    public String searchProposal() {
    	Map<String, Object> input = Map.ofEntries(
    			Map.entry("Conditions", Map.ofEntries(
    					Map.entry("ProductCode", "TBTI"))),
    			Map.entry("PageNo", 1),
    			Map.entry("PageSize", 50),
    			Map.entry("SortType", "desc"),
    			Map.entry("Module", "Policy")
    	);
    	
    	setToken();
    	Map output = proposal.query(input);
    	logger.info(mapToJsonString(output));

    	logger.info("Total search results=" + output.get("Total").toString());
    	Map result = (Map)((List)output.get("Results")).get(0);
    	List<Map> esDocs = (List<Map>)result.get("EsDocs");
    	for (Map doc : esDocs ) {
    		String policyNo = doc.get("PolicyNo") == null ? "" : doc.get("PolicyNo").toString();
    		String proposalNo = doc.get("ProposalNo") == null ? "" : doc.get("ProposalNo").toString();
    		logger.info("ProposalNo=" + proposalNo + "   PolicyNo=" + policyNo);
    	}
    	return output.get("Total").toString();
    }
    
    @GetMapping(value = "/load")
    public String loadProposal(@RequestParam(value="proposalNo", required = false) String proposalNo) {
    	if(proposalNo== null ||  proposalNo.trim().length()<1) {
    		return "example: /load?proposalNo=PABTBTI0000000054";
    	}
    	
    	setToken();
    	Map output = proposal.load(proposalNo, null, null, null);
    	logger.info(mapToJsonString(output));
    	return mapToJsonString(output);	
    }
    
    
    /**
     * Most GI proposal and quotation APIs expect policy structure for the particular insurance product 
     * as input. Refer to the details of the technical product for the policy structure. 
     * You can also use the the 'Sample payload' feature to generate sample JSON format policy structure. 
     * The feature can be access from two places 
     * 			1. product factory -> market product -> Sample payload
     * 			2. insuremo console -> product -> story -> related APIs -> Sample payload 

     * @return a map object contains policy structure for the TBTI sample product
     * 
     */
    private Map getProposalModel() {
    	return Map.ofEntries(
       			Map.entry("EffectiveDate", "2019-04-22"),
       			Map.entry("ExpiryDate", "2019-04-24T23:59:59"),
       			Map.entry("IsPremiumCalcSuccess", "Y"),
       			Map.entry("PolicyType", "1"),
       			Map.entry("ProductCode", "TBTI"),
       			Map.entry("ProductId", 351925022L),
       			Map.entry("ProductVersion", "1.0"),
       			Map.entry("ProposalDate", "2019-04-22"),
       			Map.entry("TechProductCode", "TR_POC"),
       			Map.entry("TechProductId", 3516410623456L),
       		 
       			Map.entry("PolicyCustomerList", new Map[]{
       					Map.ofEntries(
	   						Map.entry("CustomerName", "Tony"),
	   						Map.entry("DateOfBirth", "1958-10-01"),
	       	       			Map.entry("IdNo", "31000000000001"),
	       	       			Map.entry("IdType", "1"),
	       	       			Map.entry("IsInsured", "N"),
	       	       			Map.entry("IsOrgParty", "N"),
	       	       			Map.entry("IsPolicyHolder", "Y"),
	       	       			Map.entry("VersionSeq", 1)
    					)}),
       			Map.entry("PolicyLobList", new Map[]{
       					Map.ofEntries(
	       	       			Map.entry("PolicyRiskList",  
	       	       				new Map[]{
	       	        					Map.ofEntries(
	       	 	   						Map.entry("CustomerName", "Tony"),
	       	 	   						Map.entry("DateOfBirth", "1958-10-01"),
	       	 	       	       			Map.entry("IdNo", "31000000000001"),
	       	 	       	       			Map.entry("ProductElementCode", "R10007"),
	       	 	       	       			Map.entry("RiskName", "InsuredName"),
	       	 	       	       			Map.entry("VersionSeq", 1),
			       	 	       	       	Map.entry("PolicyCoverageList", new Map[]{
			       	        					Map.ofEntries(
					       	 	   						Map.entry("DuePremium", 100),
					       	 	   						Map.entry("ProductElementCode", "C100416"),
					       	 	       	       			Map.entry("SumInsured", 300000),
					       	 	       	       			Map.entry("VersionSeq", 1)),
			       	        					Map.ofEntries(
					       	 	   						Map.entry("DuePremium", 100),
					       	 	   						Map.entry("ProductElementCode", "C100692"),
					       	 	       	       			Map.entry("SumInsured", 300000),
					       	 	       	       			Map.entry("VersionSeq", 1)),
			       	        					Map.ofEntries(
					       	 	   						Map.entry("DuePremium", 20),
					       	 	   						Map.entry("ProductElementCode", "C100715"),
					       	 	       	       			Map.entry("SumInsured", 300000),
					       	 	       	       			Map.entry("VersionSeq", 1))
			       	        
			       	        					})
	       	 	       	       			
	       	     					)}
	       	       			),
	       	       			Map.entry("ProductCode", "TBTI"),
	       	       			Map.entry("ProductElementCode", "TBTI"),
	       	       			Map.entry("ProductId", 351925022L),
	       	       			Map.entry("ProductLobId", 351925023L),
	       	       			Map.entry("TechProductCode", "TR_POC"),
	       	       			Map.entry("TechProductId", 3516410623456L),
	       	       			Map.entry("TotalInsuredCount", 1),
	       	       			Map.entry("VersionSeq", 1)
	       	       			
    					)})
    	);
    }
    
    /**
     * Set the token used to call GI APIs. A token represent an individual user or 
     * a system. The user must be granted access in GI to use the corresponding APIs.   
     * 
     * Individual user token is normally used in a system where each user has his/her own
     * account in InsureMO URP system. The user activities and API calls can be traced back to
     * the individual user. Individual token is received by calling the the CAS service
     * with the user credential and is valid for a short period of time, typically 
     * a few hours. For example, in a claim system, each agent login to the claim application 
     * using user name and password. The claim application calls CAS service to get individual 
     * token for the user and use the token for all subsequent GI API calls.  
     * 
     * System user token is normally used where users are not managed in InsureMO, or
     * no individual user is involved. An system user must be created in InsureMO to represent
     * the caller system and access must be granted. Then use InsureMo -> API Management 
     * -> Access Token Management to create a token for the system user. The token should be 
     * securely stored, for example in config center, and is used to make API calls. 
     * System user token is also called long token for its long lifetime. 
     * 
     * @param 
     */
    private void setToken() {
    	if(ThreadContext.getThreadInfo() != null) {
    		ThreadContext.getThreadInfo().put(PlatFormConstant.HEADER_AUTHORIZATION, ebaoGIToken);
    	}
    }
    
    
    /**
     * 
     * @param map
     * @return JSON String
     */
    private String mapToJsonString(Map map) {
    	String json ;
        try {
			json = new ObjectMapper().writeValueAsString(map);
		} catch (JsonProcessingException e) {
			json = e.getMessage();
			e.printStackTrace();
		}
        return json;
    }
}
