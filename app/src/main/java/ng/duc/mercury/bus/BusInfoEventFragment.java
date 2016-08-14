package ng.duc.mercury.bus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ng.duc.mercury.AppConstants.BUSINESS_ACTIVITY;
import ng.duc.mercury.R;

/**
 * Created by ducnguyen on 8/11/16.
 * Fragment that shows a single event in business info activity
 */
public class BusInfoEventFragment extends Fragment {

	private static final String LOG_TAG = BusInfoEventFragment.class.getSimpleName();

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

		Bundle arg = getArguments();
		String busId;
		if (arg != null) {
			busId = arg.getString(BUSINESS_ACTIVITY.BUNDLE_BUS_ID);
		}

		return inflater.inflate(R.layout.fragment_bus_info_event, container, false);

	}
}
