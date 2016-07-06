package ng.duc.mercury;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by ducnguyen on 7/1/16.
 * Test suite for the program's Utility functions
 */
@RunWith(AndroidJUnit4.class)
public class UtilityAndroidTest {

	@Test
	public void buildUrl() {

		assertThat(Utility.BuildURL.tagSync("C3490F", 5).toString(),
				   is("https://www.mercury.com/tag?userId=C3490F&extra=5"));

	}

}
