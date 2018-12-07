package com.lambda.search.service;

import java.util.Properties;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;

public interface SearchService {

	public SearchRequest prepareSearchQuery(Object input);
	public String processResults(SearchResult searchResult, Object input);
	public AmazonCloudSearchDomain getAWSCredentialsAndCSDomainClient(Properties prop);
	public Properties loadPropertyFile(String file);
	public AmazonCloudSearchDomain getAWSCredentialsAndCSDomainClient();
	public SearchResult executeSearch(SearchRequest request, AmazonCloudSearchDomain csClient);
}
