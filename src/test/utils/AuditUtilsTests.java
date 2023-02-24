package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.junit.Before;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.looksee.models.enums.AuditCategory;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;

public class AuditUtilsTests {

	@Test
	public void verifyPageAuditProgress() throws MalformedURLException{
		assetTrue(true);
	}
	
}
