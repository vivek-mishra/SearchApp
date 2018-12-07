package com.lambda.search;

import java.util.Properties;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.lambda.search.constants.Constant;
import com.lambda.search.service.SearchService;
import com.lambda.search.service.SearchServiceImpl;

public class LambdaFunctionHandler implements RequestHandler<Object, String> {

	private SearchService searchServiceImpl = new SearchServiceImpl();

	public String handleRequest(Object input, Context context) {
		Properties prop = searchServiceImpl.loadPropertyFile(Constant.PATH);
		String documents = null;
		// Get AWS CS Domain Client and use it for CloudSearch request
		AmazonCloudSearchDomain csClient = searchServiceImpl.getAWSCredentialsAndCSDomainClient(prop);

		// preparing search query and printing out the request out
		SearchRequest request = searchServiceImpl.prepareSearchQuery(input);

		// invoking search method of CSDomainClient to get results from indexed
		// documents
		if (csClient != null) {
			SearchResult searchResult = searchServiceImpl.executeSearch(request, csClient);
			// processing search results here, passing searched result and input
			// as
			// params
			documents = searchServiceImpl.processResults(searchResult, input);
			context.getLogger().log("matched results " + documents);
			return documents != null ? documents : "no results found for query" + input.toString();
		}
		return "Can't perform search, Cloudsearch domain client not available" + csClient;
	}

}
