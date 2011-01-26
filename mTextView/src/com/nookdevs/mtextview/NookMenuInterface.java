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

package com.nookdevs.mtextview;

/*
 * This defines the functions an Activity must implement in order
 * to use the NookMenu class.
 */

public interface NookMenuInterface {

	//  Called when the user clicks a menu item:
	void nookMenuOnClickAction( int menuID, int itemID );

	//  Returns the resource ID of an icon for this menu item, or null for no icon:
	Integer nookMenuGetItemIcon( int menuID, int itemID );

	//  Returns the label of this menu item:
	public String nookMenuGetItemLabel( int menuID, int itemID );
}