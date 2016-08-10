package ng.duc.mercury;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ducnguyen on 8/4/16.
 * This class:
 *      - will be called first when the application launched
 *      - can make application-wise change
 */
public class Application extends android.app.Application {

	private static final String LOG_TAG = Application.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();

		// A little trick here, since Android does not support many fonts
		// especially early APIs (<21), we have to use various versions
		// of Avenir incorrectly. This will avoid memory leak.

		Map<String, String> fontMap = new HashMap<>();
		fontMap.put("sans-serif", "fonts/AvenirLTStd-Light.otf");
		fontMap.put("monospace", "fonts/AvenirLTStd-Book.otf");
		fontMap.put("serif", "fonts/AvenirLTStd-Heavy.otf");

		Utility.setDefaultFont(this, fontMap);
	}
}
