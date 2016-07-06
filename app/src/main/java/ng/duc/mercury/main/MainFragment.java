package ng.duc.mercury.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ng.duc.mercury.R;
import ng.duc.mercury.custom_views.MainViewPager;

/**
 * Created by ducnguyen on 6/14/16.
 * This class is responsible for loading data from the database.
 */
public class MainFragment extends Fragment {

	private static final String LOG_TAG = MainFragment.class.getSimpleName();

//	public static final int LOADER_PERSONAL = 0;
//	public static final int LOADER_RECOMMEND = 1;
//	public static final int LOADER_AROUND = 2;
//
//	private Uri mUri;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_main);
		MainViewPager mViewPager = (MainViewPager) rootView.findViewById(R.id.viewpager_main);
		TabLayout mTab = (TabLayout) rootView.findViewById(R.id.tablayout_main);

		MainPagerAdapter mPagerAdapter = new MainPagerAdapter(getChildFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);
		mTab.setupWithViewPager(mViewPager);

		((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

		return rootView;
	}

	public static class MainPagerAdapter extends FragmentStatePagerAdapter {

		static final int NUM_ITEMS = 3;
		static final int PERSONAL_PAGE = 0;
		static final int RECOMMEND_PAGE = 1;
		static final int AROUND_PAGE = 2;

		public MainPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {

			switch(position) {
				case PERSONAL_PAGE:
					return new PersonalFragment();

				case RECOMMEND_PAGE:
					return new RecommendFragment();

				case AROUND_PAGE:
					return new AroundFragment();

				default:
					throw new UnsupportedOperationException("There is no " +
						"position: " + String.valueOf(position));
			}
		}

		@Override
		public int getCount() {
			return NUM_ITEMS;
		}

		@Override
		public CharSequence getPageTitle(int position) {

			switch(position) {

				case PERSONAL_PAGE:
					return "Personal";

				case RECOMMEND_PAGE:
					return "Recommend";

				case AROUND_PAGE:
					return "Around";

				default:
					throw new UnsupportedOperationException(
							"There is no position: " + String.valueOf(position));
			}
		}
	}

}
