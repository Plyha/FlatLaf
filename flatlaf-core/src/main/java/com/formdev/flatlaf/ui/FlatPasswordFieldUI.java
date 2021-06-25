/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import com.formdev.flatlaf.icons.FlatCapsLockIcon;
import com.formdev.flatlaf.ui.FlatStyleSupport.Styleable;
import com.formdev.flatlaf.util.HiDPIUtils;

/**
 * Provides the Flat LaF UI delegate for {@link javax.swing.JPasswordField}.
 *
 * <!-- BasicPasswordFieldUI -->
 *
 * @uiDefault PasswordField.font					Font
 * @uiDefault PasswordField.background				Color
 * @uiDefault PasswordField.foreground				Color	also used if not editable
 * @uiDefault PasswordField.caretForeground			Color
 * @uiDefault PasswordField.selectionBackground		Color
 * @uiDefault PasswordField.selectionForeground		Color
 * @uiDefault PasswordField.disabledBackground		Color	used if not enabled
 * @uiDefault PasswordField.inactiveBackground		Color	used if not editable
 * @uiDefault PasswordField.inactiveForeground		Color	used if not enabled (yes, this is confusing; this should be named disabledForeground)
 * @uiDefault PasswordField.border					Border
 * @uiDefault PasswordField.margin					Insets
 * @uiDefault PasswordField.echoChar				character
 * @uiDefault PasswordField.caretBlinkRate			int		default is 500 milliseconds
 *
 * <!-- FlatPasswordFieldUI -->
 *
 * @uiDefault Component.minimumWidth				int
 * @uiDefault Component.isIntelliJTheme				boolean
 * @uiDefault PasswordField.placeholderForeground	Color
 * @uiDefault PasswordField.focusedBackground		Color	optional
 * @uiDefault PasswordField.showCapsLock			boolean
 * @uiDefault PasswordField.capsLockIcon			Icon
 * @uiDefault TextComponent.selectAllOnFocusPolicy	String	never, once (default) or always
 * @uiDefault TextComponent.selectAllOnMouseClick	boolean
 *
 * @author Karl Tauber
 */
public class FlatPasswordFieldUI
	extends BasicPasswordFieldUI
{
	@Styleable protected int minimumWidth;
	protected boolean isIntelliJTheme;
	private Color background;
	@Styleable protected Color disabledBackground;
	@Styleable protected Color inactiveBackground;
	@Styleable protected Color placeholderForeground;
	@Styleable protected Color focusedBackground;
	@Styleable protected boolean showCapsLock;
	protected Icon capsLockIcon;

	private Color oldDisabledBackground;
	private Color oldInactiveBackground;

	private FocusListener focusListener;
	private KeyListener capsLockListener;
	private Map<String, Object> oldStyleValues;
	private AtomicBoolean borderShared;
	private boolean capsLockIconShared = true;

	public static ComponentUI createUI( JComponent c ) {
		return new FlatPasswordFieldUI();
	}

	@Override
	public void installUI( JComponent c ) {
		super.installUI( c );

		applyStyle( FlatStyleSupport.getStyle( c ) );
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();

		String prefix = getPropertyPrefix();
		minimumWidth = UIManager.getInt( "Component.minimumWidth" );
		isIntelliJTheme = UIManager.getBoolean( "Component.isIntelliJTheme" );
		background = UIManager.getColor( prefix + ".background" );
		disabledBackground = UIManager.getColor( prefix + ".disabledBackground" );
		inactiveBackground = UIManager.getColor( prefix + ".inactiveBackground" );
		placeholderForeground = UIManager.getColor( prefix + ".placeholderForeground" );
		focusedBackground = UIManager.getColor( prefix + ".focusedBackground" );
		showCapsLock = UIManager.getBoolean( "PasswordField.showCapsLock" );
		capsLockIcon = UIManager.getIcon( "PasswordField.capsLockIcon" );

		LookAndFeel.installProperty( getComponent(), "opaque", false );

		MigLayoutVisualPadding.install( getComponent() );
	}

	@Override
	protected void uninstallDefaults() {
		super.uninstallDefaults();

		background = null;
		disabledBackground = null;
		inactiveBackground = null;
		placeholderForeground = null;
		focusedBackground = null;
		capsLockIcon = null;

		oldDisabledBackground = null;
		oldInactiveBackground = null;

		oldStyleValues = null;
		borderShared = null;

		MigLayoutVisualPadding.uninstall( getComponent() );
	}

	@Override
	protected void installListeners() {
		super.installListeners();

		// necessary to update focus border and background
		focusListener = new FlatUIUtils.RepaintFocusListener( getComponent(), null );

		// update caps lock indicator
		capsLockListener = new KeyAdapter() {
			@Override
			public void keyPressed( KeyEvent e ) {
				repaint( e );
			}
			@Override
			public void keyReleased( KeyEvent e ) {
				repaint( e );
			}
			private void repaint( KeyEvent e ) {
				if( e.getKeyCode() == KeyEvent.VK_CAPS_LOCK )
					e.getComponent().repaint();
			}
		};

		getComponent().addFocusListener( focusListener );
		getComponent().addKeyListener( capsLockListener );
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();

		getComponent().removeFocusListener( focusListener );
		getComponent().removeKeyListener( capsLockListener );
		focusListener = null;
		capsLockListener = null;
	}

	@Override
	protected Caret createCaret() {
		return new FlatCaret( UIManager.getString( "TextComponent.selectAllOnFocusPolicy" ),
			UIManager.getBoolean( "TextComponent.selectAllOnMouseClick" ) );
	}

	@Override
	protected void propertyChange( PropertyChangeEvent e ) {
		String propertyName = e.getPropertyName();
		if( "editable".equals( propertyName ) || "enabled".equals( propertyName ) )
			updateBackground();
		else
			super.propertyChange( e );
		FlatTextFieldUI.propertyChange( getComponent(), e, this::applyStyle );
	}

	/**
	 * @since TODO
	 */
	protected void applyStyle( Object style ) {
		oldDisabledBackground = disabledBackground;
		oldInactiveBackground = inactiveBackground;

		oldStyleValues = FlatStyleSupport.parseAndApply( oldStyleValues, style, this::applyStyleProperty );

		updateBackground();
	}

	/**
	 * @since TODO
	 */
	protected Object applyStyleProperty( String key, Object value ) {
		if( key.equals( "capsLockIconColor" ) && capsLockIcon instanceof FlatCapsLockIcon ) {
			if( capsLockIconShared ) {
				capsLockIcon = FlatStyleSupport.cloneIcon( capsLockIcon );
				capsLockIconShared = false;
			}
			return ((FlatCapsLockIcon)capsLockIcon).applyStyleProperty( key, value );
		}

		if( borderShared == null )
			borderShared = new AtomicBoolean( true );
		return FlatStyleSupport.applyToAnnotatedObjectOrBorder( this, key, value, getComponent(), borderShared );
	}

	private void updateBackground() {
		FlatTextFieldUI.updateBackground( getComponent(), background,
			disabledBackground, inactiveBackground,
			oldDisabledBackground, oldInactiveBackground );
	}

	@Override
	protected void paintSafely( Graphics g ) {
		FlatTextFieldUI.paintBackground( g, getComponent(), isIntelliJTheme, focusedBackground );
		FlatTextFieldUI.paintPlaceholder( g, getComponent(), placeholderForeground );
		paintCapsLock( g );

		super.paintSafely( HiDPIUtils.createGraphicsTextYCorrection( (Graphics2D) g ) );
	}

	protected void paintCapsLock( Graphics g ) {
		if( !showCapsLock )
			return;

		JTextComponent c = getComponent();
		if( !FlatUIUtils.isPermanentFocusOwner( c ) ||
			!Toolkit.getDefaultToolkit().getLockingKeyState( KeyEvent.VK_CAPS_LOCK ) )
		  return;

		int y = (c.getHeight() - capsLockIcon.getIconHeight()) / 2;
		int x = c.getWidth() - capsLockIcon.getIconWidth() - y;
		capsLockIcon.paintIcon( c, g, x, y );
	}

	@Override
	protected void paintBackground( Graphics g ) {
		// background is painted elsewhere
	}

	@Override
	public Dimension getPreferredSize( JComponent c ) {
		return FlatTextFieldUI.applyMinimumWidth( c, super.getPreferredSize( c ), minimumWidth );
	}

	@Override
	public Dimension getMinimumSize( JComponent c ) {
		return FlatTextFieldUI.applyMinimumWidth( c, super.getMinimumSize( c ), minimumWidth );
	}
}
