package ng.duc.mercury.custom_views;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by ducnguyen on 7/31/16.
 * This class simulates an image carousel. Image Carousel is a simple object that simulates an
 * image carousel. Image carousel visually looks like this:
 *
 * ____________________________________________________________
 * __     ___________     ___________     ___________     _____
 *   |   |           |   |           |   |           |   |
 *   |   |           |   |           |   |           |   |
 * 0 |   |  Image 1  |   |  Image 2  |   |  Image 3  |   |  Image 4
 *   |   |           |   |           |   |           |   |
 * __|   |___________|   |___________|   |___________|   |______
 * _____________________________________________________________
 *
 * To function, this class requires:
 *      - each image data: ArrayList<String>, with each string corresponds to a piece of
 *      information in a single image (because other than pure image, each image can contain
 *      clickable text, button, or decorating views). The whole data on all images will be
 *      ArrayList<ArrayList<String>>
 *      - each image uri (optional): String. Each image uri is a string, so the whole collection
 *       of uris for all image is ArrayList<String>
 *      - layout of each image: int. The layout that contains all views within a single image.
 *      - each image layout ids: ArrayList<String>. Since each piece of information in the image
 *      must be assigned to a view, ImageCarousel needs the ids to accurately assign piece of
 *      information in image data to the view. For simplicity, each piece of information
 *      in data corresponds to the respective ids. E.g.: each image data: ["abc", "def", "ghi"],
 *      layout ids: [R.id.text1, R.id.text2, R.id.text3] -> "abc" will be assigned to id.text1,
 *      "def" will be assigned to id.text2, and "ghi" will be assigned to id.text3. Therefore,
 *      be cautious when using this object.
 *      - data types: can either be text or image. This is necessary for ImageCarousel know which
 *      view in the layout is TextView and which is ImageView to cast successfully.
 */
public class ImageCarousel extends RecyclerView {

	private ArrayList<ArrayList<String>> mData;
	private int mLayouts;
	private ArrayList<Integer> mIds;
	private ArrayList<String> mTypes;
	private ArrayList<String> mUri;

	Typeface avenirBook;
	Typeface avenirHeavy;
	Typeface avenirLight;

	private ImageCarouselAdapter mAdapter;

	public ImageCarousel(Context context) {
		this(context, null);
	}

	public ImageCarousel(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageCarousel(Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setLayout(int layout) {
		mLayouts = layout;
	}

	public void setData(ArrayList<ArrayList<String>> data) {
		mData = data;
	}

	public void setIds(ArrayList<Integer> ids) {
		mIds = ids;
	}

	public void setType(ArrayList<String> type) {
		mTypes = type;
	}

	public void setUri(ArrayList<String> uri) {
		mUri = uri;
	}

	public void runCarousel() {

		setItemViewCacheSize(12);
		setLayoutManager(new LinearLayoutManager(
				getContext(),
				LinearLayoutManager.HORIZONTAL,
				false
		));


		// TODO: might need a way to make this font accessible across the application, otherwise large memory overhead
		avenirBook = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/AvenirLTStd-Book.otf");
		avenirHeavy = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/AvenirLTStd-Heavy.otf");
		avenirLight = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/AvenirLTStd-Light.otf");

		mAdapter = new ImageCarouselAdapter(mData, mLayouts, mIds, mTypes, mUri);
		setAdapter(mAdapter);

	}


	/**
	 * Following is the way all input data are structured:
	 *      - data: an array of image details, each image detail is also an array of information
	 *      (image url, image name, image subtitle...). An N-length data is equal to a recycler
	 *      view of N items
	 *      - uri: N-length, each uri corresponds to the uri for the data
	 *      - layout: a layout Id that each of the image will use
	 *      - ids: the view ids of each view in the layout. If an image detail contains N pieces
	 *      of information, then length of ids is 5, and each id in ids is respectively
	 *      corresponds to a piece of information in image detail
	 *      - types: the types of views in ids. Types can be: "image" (for ImageView) or "text"
	 *      (for TextView)
	 *  TODO: might need to pass the activity to be clicked so that we can create an explicit
	 *  intent for click-binding event.
	 */
	public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ViewHolder> {

		private ArrayList<ArrayList<String>> mData;
		private ArrayList<String> mUri;
		private int mLayout = -1;
		private ArrayList<Integer> mIds;
		private ArrayList<String> mTypes;

		private Picasso picasso;

		private final String LOG_TAG = ImageCarouselAdapter.class.getSimpleName();


		public class ViewHolder extends RecyclerView.ViewHolder {

			private ArrayList<View> views;

			public ViewHolder(View v) {
				super(v);
				views = new ArrayList<>();
				findViews();
			}

			public void findViews() {

				for (int id : mIds) {
					views.add(itemView.findViewById(id));
				}
			}

			public ArrayList<View> getViews() {
				return views;
			}

			public void bindClickEvent(final String uri) {

				// Later will require a separate onclick callback interface, given
				// a wide range of types of clicks that we want to handle

				itemView.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast.makeText(getContext(), "Item: " + uri, Toast.LENGTH_SHORT)
								.show();
					}
				});
			}

		}

		public ImageCarouselAdapter(ArrayList<ArrayList<String>> data, int layout,
		                            ArrayList<Integer> ids, ArrayList<String> types,
									@Nullable ArrayList<String> uri) {
			mData = data;
			mLayout = layout;
			mIds = ids;
			mTypes = types;
			mUri = uri;

			picasso = Picasso.with(getContext());
			picasso.setIndicatorsEnabled(true);
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

			View view = LayoutInflater.from(parent.getContext())
					.inflate(mLayout, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, int position) {

			if (mUri != null) {
				holder.bindClickEvent(mUri.get(position));
			}

			ArrayList<View> views = holder.getViews();

			for (int i = 0; i < views.size(); i++) {

				switch (mTypes.get(i)) {

					case "image":
						picasso.load(mData.get(position).get(i))
								.fit()
								.centerCrop()
								.into((ImageView) views.get(i));
						break;

					case "text":
						((TextView) views.get(i)).setText(mData.get(position).get(i));
						// TODO: setting typeface in onBind view is not efficient, since it
						// will be called many times. Some way to set typeface in view holder
						// would be much better.
						((TextView) views.get(i)).setTypeface(avenirLight);
						break;

					default:
						throw new UnsupportedOperationException(
								"Unknown type in " + LOG_TAG + ": " + mTypes.get(i));

				}

			}

		}

		@Override
		public int getItemCount() {
			return mData.size();
		}
	}
}
