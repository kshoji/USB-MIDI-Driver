package jp.kshoji.driver.midi.sample.view;

import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * The {@link ArrayAdapter} which has a size limit.
 * 
 * @author K.Shoji
 * 
 * @param <T>
 */
public class LimitedArrayAdapter<T> extends ArrayAdapter<T> {
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int textViewResourceId, int limitSize) {
		super(context, textViewResourceId);
		this.limitSize = limitSize;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param resource
	 * @param textViewResourceId
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int resource, int textViewResourceId, int limitSize) {
		super(context, resource, textViewResourceId);
		this.limitSize = limitSize;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param resource
	 * @param textViewResourceId
	 * @param objects
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int resource, int textViewResourceId, List<T> objects, int limitSize) {
		super(context, resource, textViewResourceId, objects);
		this.limitSize = limitSize;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param resource
	 * @param textViewResourceId
	 * @param objects
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects, int limitSize) {
		super(context, resource, textViewResourceId, objects);
		this.limitSize = limitSize;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int textViewResourceId, List<T> objects, int limitSize) {
		super(context, textViewResourceId, objects);
		this.limitSize = limitSize;
	}

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 * @param limitSize
	 */
	public LimitedArrayAdapter(Context context, int textViewResourceId, T[] objects, int limitSize) {
		super(context, textViewResourceId, objects);
		this.limitSize = limitSize;
	}

	private final int limitSize;
	
	/*
	 * (non-Javadoc)
	 * @see android.widget.ArrayAdapter#add(java.lang.Object)
	 */
	@Override
	public void add(T object) {
		super.add(object);

		synchronized (this) {
			while (getCount() > limitSize) {
				remove(getItem(getCount() - 1));
			}
		}
	}
}
