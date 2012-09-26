package com.streamhub;

import java.io.File;
import java.net.URL;

import com.streamhub.api.PushServer;
import com.streamhub.nio.NIOServer;
import com.streamhub.util.StreamUtils;
import com.streamhub.util.UrlLoader;

public class UrlLoaderTest extends StreamingServerTestCase {
	public static final String META_TAGS = "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"><meta http-equiv=\"Cache-Control\" content=\"no-store\"><meta http-equiv=\"Cache-Control\" content=\"no-cache\"><meta http-equiv=\"Pragma\" content=\"no-cache\"><meta http-equiv=\"Expires\" content=\"Thu, 1 Jan 1970 00:00:00 GMT\"><title>StreamHub Push Page</title>";
	private PushServer server;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		server = new NIOServer(8888);
		server.start();
		server.addStaticContent(new File("src/test/resources/static"));
	}

	@Override
	protected void tearDown() throws Exception {
		server.addStaticContent(null, null);
		server.stop();
		super.tearDown();
	}

	public void testOpeningStreamFromHttpUrl() throws Exception {
		URL licenseUrl = new URL("http://localhost:8888/load-tests/license.txt");
		String licenseAsString = StreamUtils.toString(licenseUrl.openStream());
		assertEquals("--blah--\r\n==hash==", licenseAsString);
	}
	
	public void testOpeningStreamFromFileUrl() throws Exception {
		File currentDir = new File(".");
		String fileUrl = "file:///" + currentDir.getAbsolutePath() + "/src/test/resources/static/load-tests/license.txt";
		URL licenseUrl = UrlLoader.load(fileUrl);
		String licenseAsString = StreamUtils.toString(licenseUrl.openStream());
		assertEquals("--blah--\r\n==hash==", licenseAsString);
	}
	
	public void testOpeningStreamFromJarUrl() throws Exception {
		File currentDir = new File(".");
		String jarUrl = "jar:file:///" + currentDir.getAbsolutePath() + "/src/test/resources/static/load-tests/license.jar!/license.txt";
		URL licenseUrl = UrlLoader.load(jarUrl);
		String licenseAsString = StreamUtils.toString(licenseUrl.openStream());
		assertEquals("--blah--\r\n==hash==", licenseAsString);
	}
	
	public void testOpeningStreamFromRemoteJarUrl() throws Exception {
		File currentDir = new File(".");
		String jarUrl = "jar:file:///" + currentDir.getAbsolutePath() + "/src/test/resources/static/load-tests/license.jar!/license.txt";
		URL licenseUrl = UrlLoader.load(jarUrl);
		String licenseAsString = StreamUtils.toString(licenseUrl.openStream());
		assertEquals("--blah--\r\n==hash==", licenseAsString);
	}
	
	public void testCreatingUrlFromClasspathUrl() throws Exception {
		String classpathUrl = "classpath:/com/streamhub/Connection.class";
		URL classUrl = UrlLoader.load(classpathUrl);
		assertNotNull(classUrl);
	}
	
	public void testOpeningStreamFromClasspathUrl() throws Exception {
		String classpathUrl = "classpath:/index.html";
		URL htmlUrl = UrlLoader.load(classpathUrl);
		String indexFileAsString = StreamUtils.toString(htmlUrl.openStream());
		assertTrue(indexFileAsString.length() > 0);
		assertTrue(indexFileAsString.contains("html"));
	}
	
	public void testOpeningStreamFromNoLeadingSlashClasspathUrl() throws Exception {
		String classpathUrl = "classpath:index.html";
		URL htmlUrl = UrlLoader.load(classpathUrl);
		String indexFileAsString = StreamUtils.toString(htmlUrl.openStream());
		assertTrue(indexFileAsString.length() > 0);
		assertTrue(indexFileAsString.contains("html"));
	}
}
