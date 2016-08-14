package ng.duc.mercury.custom_views;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ng.duc.mercury.R;

/**
 * Created by ducnguyen on 8/13/16.
 * This view contains a single header and a list of texts, like as follow:
 * Header:
 *      Text 1              Sub 1
 *      Text 2              Sub 2
 *      Text 3              Sub 3
 *
 * To use, follow the following steps:
 * 1.
 */
public class HeaderListText extends LinearLayout {

	private static final String LOG_TAG = HeaderListText.class.getSimpleName();

	private ArrayList<String> mDataLeft = null;
	private ArrayList<String> mDataRight = null;
	private ArrayList<String> mUris = null;
	private String mHeader = null;

	private static final int TYPE_1_LEFT = 0;
	private static final int TYPE_1_RIGHT = 1;
	private static final int TYPE_2 = 2;

	private int type;

	public HeaderListText(Context context) {
		this(context, null);
	}

	public HeaderListText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public HeaderListText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(21)
	public HeaderListText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void addDataLeft(String dataLeft) {

		if (mDataLeft == null) {
			mDataLeft = new ArrayList<>();
		}

		mDataLeft.add(dataLeft);
	}

	public void addDataRight(String dataRight) {

		if (mDataRight == null) {
			mDataRight = new ArrayList<>();
		}

		mDataRight.add(dataRight);
	}

	public void addUri(String uri) {

		if (mUris == null) {
			mUris = new ArrayList<>();
		}

		mUris.add(uri);
	}

	public void init() {
		this.setOrientation(VERTICAL);
		this.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	public void setDataLeft(ArrayList<String> data) {
		mDataLeft = data;
	}

	public void setDataRight(ArrayList<String> data) {
		mDataRight = data;
	}

	public void setHeader(String header) {
		mHeader = header;
	}

	public void setUris(ArrayList<String> uris) {
		mUris = uris;
	}

	public void run() {

		init();
		checkType();

		LayoutInflater inflater = LayoutInflater.from(getContext());

		TextView header = (TextView)
				inflater.inflate(R.layout.view_headerlist_header, this, false);
		header.setText(mHeader);
		this.addView(header);

		switch (type) {

			case TYPE_1_LEFT:

				for (int i=0; i<mDataLeft.size(); i++) {

					TextView childView = (TextView)
							inflater.inflate(R.layout.view_headerlisttext_item_left, this, false);
					childView.setText(mDataLeft.get(i));
					if (mUris != null) {
						final String uri = mUris.get(i);
						childView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Toast.makeText(getContext(), "Uris: " + uri, Toast.LENGTH_SHORT)
										.show();
							}
						});
					}
					this.addView(childView);
				}
				break;

			case TYPE_1_RIGHT:

				for (int i=0; i<mDataRight.size(); i++) {

					TextView childView = (TextView)
							inflater.inflate(R.layout.view_headerlisttext_item_right, this, false);
					childView.setText(mDataRight.get(i));
					if (mUris != null) {
						final String uri = mUris.get(i);
						childView.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Toast.makeText(getContext(), "Uris: " + uri, Toast.LENGTH_SHORT)
										.show();
							}
						});
					}
					this.addView(childView);
				}
				break;

			case TYPE_2:

				for (int i=0; i<mDataLeft.size(); i++) {

					LinearLayout holder = (LinearLayout) inflater
							.inflate(R.layout.view_headerlisttext_item_2_holder, this, false);
					if (mUris != null) {
						final String uri = mUris.get(i);
						holder.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Toast.makeText(getContext(), "Uris: " + uri, Toast.LENGTH_SHORT)
										.show();
							}
						});
					}

					TextView leftView = (TextView) inflater
							.inflate(R.layout.view_headerlisttext_item_left, this, false);
					leftView.setText(mDataLeft.get(i));

					TextView rightView = (TextView) inflater
							.inflate(R.layout.view_headerlisttext_item_right, this, false);
					rightView.setText(mDataRight.get(i));

					this.addView(holder);
				}
				break;
		}
	}

	public void checkType() {

		if ((mDataLeft != null) && (mDataRight != null)) {
			type = TYPE_2;
		} else if ((mDataLeft != null) && (mDataRight == null)) {
			type = TYPE_1_LEFT;
		} else if ((mDataLeft == null) && (mDataRight != null)) {
			type = TYPE_1_RIGHT;
		} else {
			throw new IllegalStateException(LOG_TAG + ": Unknown state: " + (mDataLeft == null)
									+ ", " + (mDataRight == null));
		}

	}
}
