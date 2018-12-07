package com.lambda.search.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClientBuilder;
import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.lambda.search.constants.Constant;

//Service class implementing methods to get AWS CS Domain client required to invoke search method, prepare search query
public class SearchServiceImpl implements SearchService {
	private final static Logger LOG = Logger.getLogger(SearchServiceImpl.class.getName());

	// method to prepare search query request
	@Override
	public SearchRequest prepareSearchQuery(Object input) {
		SearchRequest request = new SearchRequest().withQueryParser(QueryParser.Simple).withSort(Constant.SORT)
				.withReturn(Constant.ALL_FIELDS).withQuery(input.toString());
		request.setCursor(Constant.INITIAL_CURSOR);
		request.setSize(20L);
		LOG.info("query ::: " + request);
		return request;
	}

	// method to get AWS CS Domain client required to invoke search method
	@Override
	public AmazonCloudSearchDomain getAWSCredentialsAndCSDomainClient(Properties prop) {
		AmazonCloudSearchDomain csClient = null;
		if (prop != null) {
			csClient = getAWSCSDomain(prop);
		}
		return csClient;
	}

	// method to get AWS CS Domain client required to invoke search method
	@Override
	public AmazonCloudSearchDomain getAWSCredentialsAndCSDomainClient() {
		AmazonCloudSearchDomain csClient = null;
		Properties prop = new Properties();
		prop = loadPropertyFile(Constant.PATH);
		if (prop != null) {
			csClient = getAWSCSDomain(prop);
		}
		return csClient;
	}

	// overloaded method with properties type param passed to get AWS CS Domain client required to invoke search method
	private AmazonCloudSearchDomain getAWSCSDomain(Properties prop) {
		AmazonCloudSearchDomain csClient = null;
		try {
			AWSCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
			csClient = AmazonCloudSearchDomainClientBuilder.standard()
					.withEndpointConfiguration(new EndpointConfiguration(prop.getProperty("aws.cs.domain.endpoint"),
							prop.getProperty("aws.region")))
					.withCredentials(credentialsProvider).build();
			return csClient;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("cs.domain.endpoint: " + prop.getProperty("aws.cs.domain.endpoint")
					+ "aws.region " + prop.getProperty("aws.region"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return csClient;
	}

	// method to process results for the searched result as documents
	@Override
	public String processResults(SearchResult searchResult, Object input) {
		StringBuilder sb = new StringBuilder();
		if (searchResult != null) {
			for (Hit hit : searchResult.getHits().getHit()) {
				sb = sb.append(hit.getId()).append(" ").append(hit);
			}
		}
		return sb.toString();
	}

	// load properties like AWS CS endpoint, region from property file, can be
	// used to add and fetch other properties in the future
	@Override
	public Properties loadPropertyFile(String file) {
		Properties defaultProps = new Properties();
		InputStream input = null;
		try {
			input = getClass().getClassLoader().getResourceAsStream(file);
			// load property file under main/resources folder;
			// applicaiton.properties
			defaultProps.load(input);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return defaultProps;

	}

	@Override
	public SearchResult executeSearch(SearchRequest request, AmazonCloudSearchDomain csClient) {
		// invoking search method of CSDomainClient to get results from indexed
		// documents
		SearchResult searchResult = csClient.search(request);
		return searchResult;

	}

}
