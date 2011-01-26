/* 
 * Copyright 2010 nookDevs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *              http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

/*
 * This class manages the menus on the touchscreen
 */


package com.nookdevs.mtextview;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import java.util.HashMap;
import java.util.Stack;
import android.util.Log;


public class NookMenu {
	private ViewAnimator nookMenuAnimator ;
	private LayoutInflater inflater;
	private NookMenuInterface nookMenuClient ;
	private HashMap<Integer, int []> menus ;  // all menus
	private Stack<Integer> menuStack ;        // current menu stack


	NookMenu(NookMenuInterface parent, ViewAnimator nookMenuAnimator ) {
		this.nookMenuClient = parent ;
		this.nookMenuAnimator = nookMenuAnimator ;
		this.inflater = ((Activity)parent).getLayoutInflater() ;
		nookMenuAnimator.removeAllViews();
		menus = new HashMap<Integer, int []>() ;
		menuStack = new Stack<Integer>() ;
	}

	/////////////////////////////////////////////////////////////////////////////////


	//
	//  Setup methods:
	//

	//  Add a menu, with item IDs from 0 to (n-1):
	public void addMenu(Integer menuID, int numberOfItems) {
		int [] itemIDs = new int[numberOfItems] ;
		for( int c = 0 ; c < numberOfItems ; c++ ) {
			itemIDs[c] = c ;
		}
		menus.put(menuID, itemIDs);
	} // addMenu

	//  Add a menu, with user-specified item IDs:
	public void addMenu(Integer menuID, int[] itemIDs) {
		menus.put(menuID, itemIDs);
	} // addMenu

	//  Specify which menu is the main menu (required):
	public void setMainMenu(int menuID) {
		menuStack.add( menuID );
		nookMenuAnimator.setInAnimation( nookMenuAnimator.getContext(), R.anim.noanim);
		nookMenuAnimator.removeAllViews();
		nookMenuAnimator.addView( inflateNookMenu(menuID) );
		nookMenuAnimator.setDisplayedChild( 0 );
	} // setMainMenu

	//
	//  Navigation methods:
	//

	public void goIntoSubMenu(int menuID) {
		menuStack.push(menuID);
		nookMenuAnimator.addView( inflateNookMenu(menuID) );
		nookMenuAnimator.setInAnimation( nookMenuAnimator.getContext(), R.anim.fromright);
		nookMenuAnimator.setDisplayedChild( menuStack.size() - 1 );
	} // goIntoSubMenu

	public void goToMainMenu() {
		int mainMenuID = (Integer) nookMenuAnimator.getChildAt(0).getTag();
		View mainMenuView = nookMenuAnimator.getChildAt(0);
		nookMenuAnimator.removeAllViews();
		nookMenuAnimator.addView(mainMenuView);
		nookMenuAnimator.setInAnimation( nookMenuAnimator.getContext(), R.anim.noanim);
		nookMenuAnimator.setDisplayedChild( 0 );
		menuStack.removeAllElements();
		menuStack.push( mainMenuID );
	} // goToMainMenu

	public void goBack() {
		int thisIndex = menuStack.size() - 1 ;
		nookMenuAnimator.setInAnimation( nookMenuAnimator.getContext(), R.anim.fromleft);
		nookMenuAnimator.setDisplayedChild( thisIndex - 1 );
		nookMenuAnimator.removeViewAt( thisIndex );
		menuStack.pop();
	} // goBack

	//
	//  Misc:
	//

	//  If something changed and we should re-draw the menus:
	public void refresh() {
		for(int n = 0 ; n < menuStack.size() ; n++ ) {
			((BaseAdapter) ((ListView) nookMenuAnimator.getChildAt(n)).getAdapter()).notifyDataSetChanged();
		}
	} // refresh

	//  How deep into submenus we are:
	public int getMenuDepth() {
		return( menuStack.size() - 1 );
	} // getMenuDepth

	public int getCurrentMenuID() {
		//Integer menuID = (Integer) nookMenuAnimator.getChildAt(myMenuIndex).getTag();
		int i = (Integer) nookMenuAnimator.getDisplayedChild();
		Integer menuID = (Integer) nookMenuAnimator.getChildAt(i).getTag();
		return( menuID.intValue() );
	} // getCurrentMenuID

	/////////////////////////////////////////////////////////////////////////////////


	//  Note: menuDepth *must* be set to this view's menu depth before calling this:
	private View inflateNookMenu(int menuID) {
		ListView menu = (ListView) inflater.inflate( R.layout.listview, null );
		menu.setDividerHeight(0);
		menu.setTag(menuID);
		menu.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		menu.setAdapter( new NookMenuAdapter(nookMenuAnimator.getContext()) );
		menu.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				int menuID = (Integer)parent.getTag();
				int itemID = (menus.get(menuID))[position] ;
				nookMenuClient.nookMenuOnClickAction(menuID, itemID);
			}
		});
		return menu ;
	} // inflateNookMenu

	private void setupMenuItem( int menuID, int itemID, View v ) {
		TextView tv = (TextView) v.findViewById(R.id.ListTextView);
		String label = nookMenuClient.nookMenuGetItemLabel(menuID, itemID);
		if ( label == null ) label = "" ;
		tv.setText(label);

		ImageView icon = (ImageView) v.findViewById(R.id.ListImageView);
		Integer iconID = nookMenuClient.nookMenuGetItemIcon(menuID, itemID);
		if ( iconID == null ) {
			icon.setImageDrawable(null);
		} else {
			icon.setImageResource( iconID );
		}

		v.setTag( new Integer(itemID) );
	} // setupMenuItem


	private class NookMenuAdapter extends BaseAdapter {
		int myMenuIndex ;

		public NookMenuAdapter(Context c) {
			myMenuIndex = menuStack.size() - 1 ;
		} // NookMenuAdapter

		public int getCount() {
			int menuID = menuStack.get(myMenuIndex);
			return( menus.get(menuID).length );
		} // getCount

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout l;
			if ( convertView == null ) {
				l = (LinearLayout) inflater.inflate( R.layout.listitem, null );
			} else {
				l = (LinearLayout)convertView ;
			}
			try {
				Integer menuID = (Integer) nookMenuAnimator.getChildAt(myMenuIndex).getTag();
				if (menuID == null) return(null);
				int itemID = (menus.get(menuID))[position] ;
				setupMenuItem( menuID, itemID, l );
			} catch (Exception ex) {
				Log.d( this.toString(), "Exception: " + ex );
			}
			return l;
		} // getView

		public Object getItem(int position) {
			return position;
		} // getItem
		public long getItemId(int position) {
			return position;
		} // getItemId

	} // NookMenuAdapter

}
