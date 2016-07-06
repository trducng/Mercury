package ng.duc.mercury.data;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ducnguyen on 6/20/16.
 * Test suite to test data contract.
 */
@RunWith(AndroidJUnit4.class)
public class DataContractAndroidTest {

	@Test
	public void testTagEntryMethods() {

		String baseURL = "content://ng.duc.mercury/tag/";
		String encodedURI = "";

		assertThat(DataContract.tagEntry.buildGeneralTag(),
				is(Uri.parse("content://ng.duc.mercury/tag")));

		try {
			encodedURI = URLEncoder.encode("love Mai$._.fuck Mai", "UTF-8").replace("+", "%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertThat(DataContract.tagEntry.buildSpecificTags(new String[] {"love Mai", "fuck Mai"}),
				is(Uri.parse(baseURL + encodedURI)));

		try {
			encodedURI = URLEncoder.encode("hello Mai", "UTF-8").replace("+", "%20");
			encodedURI += "/";
			encodedURI += URLEncoder.encode("B123", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertThat(DataContract.tagEntry.buildSingleTag("hello Mai", "B123"),
				is(Uri.parse(baseURL + encodedURI)));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello"}),
				is("tag = ?"));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello", "from"}),
				is("tag = ? OR tag = ?"));

		assertThat(DataContract.tagEntry.buildConditionalQuery(new String[] {"Hello", "from", "the"}),
				is("tag = ? OR tag = ? OR tag = ?"));

		assertThat(DataContract.tagEntry.getTagNames(
				DataContract.tagEntry.buildSpecificTags(new String[] {"Mai"})),
				is(new String[] {"Mai"}));

		assertThat(DataContract.tagEntry.getTagNames(
				DataContract.tagEntry.buildSpecificTags(new String[] {"I/Duc", "love", "Mai", "a lot"})),
				is(new String[] {"I/Duc", "love", "Mai", "a lot"}));

		assertThat(DataContract.tagEntry.getTagNameAndBusID(
				DataContract.tagEntry.buildSingleTag("hello Mai", "B123")),
				is(new String[] {"hello Mai", "B123"}));
	}



}
