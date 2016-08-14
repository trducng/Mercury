package ng.duc.mercury.custom_views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import ng.duc.mercury.R;

/**
 * Created by ducnguyen on 8/13/16.
 * This view contains a single header and a list of image. This is how the view looks:
 * Header:
 *      Image 1         Image 2
 *      Image 3         Image 4
 * How to use:
 * 1. Create an instance of this object.
 * 2. Add data to the following fields:
 *          - mImages: the 140dp x 140dp image
 *          - mNames (optional): name of the products
 *          - mPrices (optional): price of the products
 *          - mUris (optional): the uris that will link to other activity when the
 *          product is clicked.
 * 3. Add string to mHeader
 * 4. Call instance.run()
 *      After calling run, this object will use the following layouts:
 *          - R.layout.view_headerlist_header: view for the header
 *          - R.layout.view_headerlistimage_holder: view for each line (Image 1     Image 2)
 *          - R.layout.view_headerlistimage_item: view for each image.
 * Note: mImages.size() === mNames.size() === mPrices.size() ==== mUris.size()
 *
 */
public class HeaderListImage extends LinearLayout {

	private static final String LOG_TAG = HeaderListImage.class.getSimpleName();

	private ArrayList<String> mImages = new ArrayList<>();
	private ArrayList<String> mNames = new ArrayList<>();
	private ArrayList<String> mPrices = new ArrayList<>();
	private ArrayList<String> mUris = new ArrayList<>();

	private String mHeader = null;

	public HeaderListImage(Context context) {
		this(context, null);
	}

	public HeaderListImage(Context context, AttributeSet attr) {
		this(context, attr, 0);
	}

	public HeaderListImage(Context context, AttributeSet attr, int defStyleAttr) {
		super(context, attr, 0);
	}

	@TargetApi(21)
	public HeaderListImage(Context context, AttributeSet attr,
	                       int defStyleAttr, int defStyleRes) {
		super(context, attr, defStyleAttr, defStyleRes);
	}

	public void addImage(String image) {
		mImages.add(image);
	}

	public void addName(String name) {
		mNames.add(name);
	}

	public void addPrice(String price) {
		mPrices.add(price);
	}

	public void addUri(String uri) {
		mUris.add(uri);
	}

	public void init() {
		this.setOrientation(VERTICAL);
		this.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	public void setHeader(String header) {
		mHeader = header;
	}

	public void setImages(ArrayList<String> images) {
		mImages = images;
	}

	public void setNames(ArrayList<String> names) {
		mNames = names;
	}

	public void setPrices(ArrayList<String> prices) {
		mPrices = prices;
	}

	public void setUris(ArrayList<String> uris) {
		mUris = uris;
	}

	public void print() {
		Log.v(LOG_TAG, mImages.toString());
		Log.v(LOG_TAG, mNames.toString());
		Log.v(LOG_TAG, mPrices.toString());
		Log.v(LOG_TAG, mUris.toString());
	}

	public void run() {

		init();

		Picasso picasso = Picasso.with(getContext());
		picasso.setIndicatorsEnabled(true);

		if ((mHeader == null) || (mImages.size() == 0)) {
			throw new IllegalStateException(LOG_TAG + ": Unrecognized mHeader - " + mHeader
										+ ", and mImages - " + mImages);
		}

		LayoutInflater inflater = LayoutInflater.from(getContext());
		TextView header = (TextView) inflater.inflate(R.layout.view_headerlist_header, this ,false);
		header.setText(mHeader);
		this.addView(header);

		LinearLayout childLayout = (LinearLayout) inflater.inflate(
				R.layout.view_headerlistimage_holder, this, false);
		for (int i=0; i<mImages.size(); i++) {

			if (i % 2 == 0) {
				childLayout = (LinearLayout) inflater.inflate(
						R.layout.view_headerlistimage_holder,
						this, false);
				Log.v(LOG_TAG, "New childlayout created");
			}

			RelativeLayout itemView = (RelativeLayout) inflater.inflate(
					R.layout.view_headerlistimage_item, childLayout, false);

			picasso.load(mImages.get(i))
					.fit()
					.centerCrop()
					.into((ImageView) itemView.findViewById(R.id.image_headerlistimage));

			if (mNames.size() != 0) {
				((TextView) itemView.findViewById(R.id.text_headerlistimage_name))
						.setText(mNames.get(i));
			}

			if (mPrices.size() != 0) {
				((TextView) itemView.findViewById(R.id.text_headerlistimage_price))
						.setText(mPrices.get(i));
			}

			if (mUris.size() != 0) {
				final String uri = mUris.get(i);
				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast.makeText(getContext(), "Uri: " + uri, Toast.LENGTH_SHORT).show();
					}
				});
			}

			childLayout.addView(itemView);
			Log.v(LOG_TAG, "itemView added to childLayout");

			if (i % 2 == 1) {
				this.addView(childLayout);
			}
		}

	}

}
