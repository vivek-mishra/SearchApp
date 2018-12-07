package com.lambda.test.search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.Hit;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.lambda.search.LambdaFunctionHandler;
import com.lambda.search.service.SearchService;
import com.lambda.search.service.SearchServiceImpl;

/**
 * Test cases for search app
 */
public class LambdaFunctionSearchServicesTest {

	private static Object input;
	private static SearchService searchService;
	private static AmazonCloudSearchDomain csClient;
	private static Properties prop = null;

	// setup before running tests, initialize required objects
	@BeforeClass
	public static void createInput() throws IOException {
		input = "Coffey";
		searchService = new SearchServiceImpl();
		prop = searchService.loadPropertyFile("application.properties");
		csClient = searchService.getAWSCredentialsAndCSDomainClient(prop);
	}

	private Context createContext() {
		TestContext ctx = new TestContext();
		ctx.setFunctionName("Search Function");
		return ctx;
	}

	// test case to test Lambda function for a given input string defined in the
	// createInput method
	@Test
	public void testLambdaFunctionHandler() {
		LambdaFunctionHandler handler = new LambdaFunctionHandler();
		Context ctx = createContext();

		String output = handler.handleRequest(input, ctx);
		Assert.assertNotNull(prop);
		Assert.assertNotNull(csClient);
		Assert.assertNotNull(output);
	}

	// test case to test lambda function for empty string input
	@Test
	public void testEmptyLambdaFunctionHandler() {
		LambdaFunctionHandler handler = new LambdaFunctionHandler();
		Context ctx = createContext();

		String output = handler.handleRequest("", ctx);
		Assert.assertNotNull(prop);
		Assert.assertNotNull(csClient);
		Assert.assertEquals(0, output.length());
	}

	/*
	 * test case a keyword appear in multiple files, returned users.json
	 * tickets.json as the input keyword matched in both these documents
	 */
	@Test
	public void testSearchResultInMultipleJsonFilesQuery() {
		SearchRequest request = searchService.prepareSearchQuery("Trinidad");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long expectedCount = 2L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("users.json tickets.json ", sb.toString());
		Assert.assertEquals(expectedCount, foundCount);
	}

	// test case for a given company name, matched in organizations.json for the
	// given input
	@Test
	public void testSearchKeywordInOrganizationsJsonFileQuery() {
		SearchRequest request = searchService.prepareSearchQuery("MegaCorp");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long expectedCount = 1L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("organizations.json ", sb.toString());
		Assert.assertEquals(expectedCount, foundCount);
	}

	// test case for a phrase match, returned tickets.json for the given input
	// phrase
	@Test
	public void testSearchKeywordInTicketsJsonFileQuery() {
		SearchRequest request = searchService.prepareSearchQuery("A Catastrophe in Korea");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long expectedCount = 1L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("tickets.json ", sb.toString());
		Assert.assertEquals(expectedCount, foundCount);
	}

	// test case for full word match, returned users.json for the given input
	// keyword
	@Test
	public void testSearchResultJsonFileQuery() {
		SearchRequest request = searchService.prepareSearchQuery("coffey");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long expectedCount = 1L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("users.json ", sb.toString());
		Assert.assertEquals(expectedCount, foundCount);
	}

	// test case for incomplete word and no results returned for the incomplete
	// word
	@Test
	public void testNOSearchResultReturnedForPartialKeywordQuery() {

		SearchRequest request = searchService.prepareSearchQuery("coff");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long i = 0L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("", sb.toString());
		Assert.assertEquals(i, foundCount);

	}

	// test case for URL got matched in organizations.json document and results
	// returned for URL
	@Test
	public void testSearchResultReturnedForURLQuery() {

		SearchRequest request = searchService
				.prepareSearchQuery("http://initech.zendesk.com/api/v2/organizations/103.json");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long i = 1L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("organizations.json ", sb.toString());
		Assert.assertEquals(i, foundCount);

	}

	/*
	 * test case Tags appear in document, returned organizations.json as the
	 * input keyword matched in this document
	 */
	@Test
	public void testSearchResultForTagKeywordQuery() {
		SearchRequest request = searchService.prepareSearchQuery("cherry");
		SearchResult searchResult = csClient.search(request);
		Long foundCount = searchResult.getHits().getFound();
		Long expectedCount = 1L;
		StringBuilder sb = getHit(searchResult);
		Assert.assertNotNull(searchResult);
		Assert.assertEquals("organizations.json ", sb.toString());
		Assert.assertEquals(expectedCount, foundCount);
	}

	// test case for AWS CS Domain client for incorrect endpoint/region, throws
	// an exception as expected behavior
	@Test(expected = IllegalArgumentException.class)
	public void testAWSCSDomain() {
		prop.setProperty("aws.cs.domain.endpoint", "");
		prop.setProperty("aws.region", "ap-southwest-2");
		csClient = searchService.getAWSCredentialsAndCSDomainClient(prop);
		Assert.assertEquals("", csClient);
	}

	// failure case for test property file, meant to fail here
	@Test(expected = RuntimeException.class)
	public void testWrongPropertyFile() {
		FileInputStream s;
		File file = new File("somefile.properties");
		Properties defaultProps = new Properties();
		try {
			s = new FileInputStream(file);
			defaultProps.load(s);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	// utility method used in test cases to get the id (document name;
	// users.json) for assertions
	private StringBuilder getHit(SearchResult searchResult) {
		StringBuilder sb = new StringBuilder();
		for (Hit hit : searchResult.getHits().getHit()) {
			sb = sb.append(hit.getId()).append(" ");
		}
		return sb;
	}

}
