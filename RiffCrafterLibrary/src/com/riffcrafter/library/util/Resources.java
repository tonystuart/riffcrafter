// Copyright 2007 Anthony F. Stuart - All rights reserved.
//
// This program and the accompanying materials are made available
// under the terms of the GNU General Public License. For other license
// options please contact the copyright owner.
//
// This program is made available on an "as is" basis, without
// warranties or conditions of any kind, either express or implied.

package com.riffcrafter.library.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//  Examples of using Label, Spinner and TextField
//
//  Item.TempoLabel.Text=Tempo: 
//  Item.TempoLabel.Type=Label
//
//  Item.TempoSpinner.ToolTip=Set tempo of current RiffCrafter file
//  Item.TempoSpinner.Type=Spinner
//  Item.TempoSpinner.Minimum=1
//  Item.TempoSpinner.Maximum=600
//  Item.TempoSpinner.StepSize=1
//  Item.TempoSpinner.DefaultValue=120
//
//  Item.ExpressionLabel.Text=Expression: 
//  Item.ExpressionLabel.Type=Label
//
//  Item.ExpressionTextField.ToolTip=Create and manipulate notes using RiffCrafter Expression Language
//  Item.ExpressionTextField.Type=TextField
//  Item.ExpressionTextField.Width=100

//  Example of retrieving and caching a ToolBar resource for subsequent interaction
//
//  private JSpinner getTempoSpinner()
//  {
//    if (tempoSpinner == null)
//    {
//      tempoSpinner = (JSpinner)Resources.find(toolBar, "TempoSpinner");
//    }
//    return tempoSpinner;
//  }

public final class Resources
{
  private static final Resources resources = new Resources();
  public final static String PROPERTIES_PATH = "resources.Resources";
  private static final String TYPE_LABEL = "Label";
  private static final String TYPE_ACTION = "Action";
  private static final String TYPE_TOGGLE = "Toggle";
  private static final String TYPE_SPINNER = "Spinner";
  private static final String TYPE_TEXTFIELD = "TextField";

  private static HashMap<String, Object> colorCache = new HashMap<String, Object>();
  private static HashMap<String, Action> actionCache = new HashMap<String, Action>();
  private ResourceBundle resourceBundle;

  private Resources()
  {
    try
    {
      resourceBundle = ResourceBundle.getBundle(PROPERTIES_PATH);
    }
    catch (MissingResourceException e)
    {
      try
      {
        resourceBundle = ResourceBundle.getBundle(PROPERTIES_PATH, Locale.US, getClass().getClassLoader());
      }
      catch (MissingResourceException e1)
      {
        throw new RuntimeException(e1);
      }
    }
  }

  public static Resources getInstance()
  {
    return resources;
  }

  public static String get(String key)
  {
    String value = null;
    try
    {
      value = getInstance().resourceBundle.getString(key);
    }
    catch (MissingResourceException e)
    {
      throw new RuntimeException(e);
    }
    return value;
  }

  public static String get(String key, String defaultValue)
  {
    String value = null;
    try
    {
      value = getInstance().resourceBundle.getString(key);
    }
    catch (MissingResourceException e)
    {
      value = defaultValue;
    }
    return value;
  }

  public static int getInt(String key)
  {
    String stringValue = get(key);
    int value = Integer.decode(stringValue);
    return value;
  }

  public static int getInt(String key, int defaultValue)
  {
    int value = defaultValue;
    String stringValue = get(key, null);
    if (stringValue != null)
    {
      value = Integer.decode(stringValue);
    }
    return value;
  }

  public static Enumeration<String> getKeys()
  {
    return getInstance().resourceBundle.getKeys();
  }

  public static JMenuBar getMenuBar(String name, ActionListener actionListener)
  {
    JMenuBar menuBar = new JMenuBar();
    JMenu menu = getMenu(name, actionListener);
    Component[] components = menu.getMenuComponents();
    for (int i = 0; i < components.length; i++)
    {
      menuBar.add(components[i]);
    }
    return menuBar;
  }

  public static JPopupMenu getPopupMenu(String name, ActionListener actionListener)
  {
    JMenu menu = getMenu(name, actionListener);
    return menu.getPopupMenu();
  }

  private static JMenu getMenu(String name, ActionListener actionListener)
  {
    JMenu menu = null;
    String itemList = Resources.get("Menu." + name + ".Items", null);
    if (itemList != null)
    {
      menu = new JMenu(name);
      menu.setMnemonic(name.charAt(0));
      String[] items = itemList.split(",");
      for (int i = 0; i < items.length; i++)
      {
        String item = items[i];
        if (item.equals("-"))
        {
          menu.addSeparator();
        }
        else
        {
          JMenu childMenu = getMenu(item, actionListener);
          if (childMenu != null)
          {
            menu.add(childMenu);
          }
          else
          {
            Component component = createMenuComponent(item, actionListener);
            menu.add(component);
          }
        }
      }
    }
    return menu;
  }

  public static JToolBar getToolBar(String name, ActionListener actionListener)
  {
    JToolBar toolBar = new JToolBar();
    String itemList = Resources.get("ToolBar." + name + ".Items");
    String[] items = itemList.split(",");
    for (int i = 0; i < items.length; i++)
    {
      String item = items[i];
      if (item.equals("-"))
      {
        toolBar.addSeparator();
      }
      else if (item.equals("--"))
      {
        toolBar.add(Box.createHorizontalGlue());
      }
      else
      {
        Component component = createToolBarComponent(item, actionListener);
        toolBar.add(component);
      }
    }
    return toolBar;
  }

  public static Component createMenuComponent(String key, ActionListener actionListener)
  {
    Item item = new Item(key);

    if (item.type.equals(TYPE_ACTION))
    {
      Action action = getAction(item, actionListener);
      JMenuItem menuItem = new JMenuItem(action);
      return menuItem;
    }
    if (item.type.equals(TYPE_TOGGLE))
    {
      Action action = getAction(item, actionListener);
      JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(action);
      return menuItem;
    }
    throw new RuntimeException("Unrecognized item type: " + item.type);
  }

  public static Action getAction(Item item, ActionListener actionListener)
  {
    String cacheKey = getActionCacheKey(item.key, actionListener);
    Action action = getCachedAction(cacheKey);
    if (action == null)
    {
      action = new ItemAction(item, actionListener);
      actionCache.put(cacheKey, action);
    }
    return action;
  }

  private static String getActionCacheKey(String key, ActionListener actionListener)
  {
    int keySuffix = 0;
    if (actionListener != null)
    {
      keySuffix = actionListener.hashCode();
    }
    String cacheKey = key + Integer.toString(keySuffix);
    return cacheKey;
  }

  public static Action getCachedAction(String key, ActionListener actionListener)
  {
    String cacheKey = getActionCacheKey(key, actionListener);
    return getCachedAction(cacheKey);
  }

  private static Action getCachedAction(String cacheKey)
  {
    Action action = actionCache.get(cacheKey);
    return action;
  }

  public static Component createToolBarComponent(String key, ActionListener actionListener)
  {
    Item item = new Item(key);

    if (item.type.equals(TYPE_LABEL))
    {
      JLabel label = new JLabel(item.text);
      label.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
      return label;
    }
    else if (item.type.equals(TYPE_ACTION))
    {
      Action action = getAction(item, actionListener);
      JButton button = new JButton(action);
      button.setRequestFocusEnabled(false);
      button.setName(item.key);
      button.setHideActionText(true);
      button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      if (item.iconRollover != null)
      {
        button.setRolloverEnabled(true);
        button.setRolloverIcon(item.iconRollover);
      }
      if (item.iconPressed != null)
      {
        button.setPressedIcon(item.iconPressed);
      }
      return button;
    }
    else if (item.type.equals(TYPE_TOGGLE))
    {
      Action action = getAction(item, actionListener);
      JToggleButton button = new JToggleButton(action);
      button.setRequestFocusEnabled(false);
      button.setName(item.key);
      button.setHideActionText(true);
      button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      return button;
    }
    else if (item.type.equals(TYPE_SPINNER))
    {
      JSpinner spinner = createSpinner(item, actionListener);
      spinner.setName(item.key);
      return spinner;
    }
    else if (item.type.equals(TYPE_TEXTFIELD))
    {
      JTextField textField = createTextField(item, actionListener);
      textField.setName(item.key);
      return textField;
    }

    throw new RuntimeException("Unrecognized item type: " + item.type);
  }

  private static JSpinner createSpinner(final Item item, final ActionListener actionListener)
  {
    int minimum = getInt(item.itemKey + "Minimum", 1);
    int maximum = getInt(item.itemKey + "Maximum", 100);
    int stepSize = getInt(item.itemKey + "StepSize", 1);
    int defaultValue = getInt(item.itemKey + "DefaultValue", 50);

    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(defaultValue, minimum, maximum, stepSize);
    JSpinner spinner = new JSpinner(spinnerModel);
    spinner.setToolTipText(item.toolTip);
    spinner.setMaximumSize(spinner.getMinimumSize());
    spinner.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent e)
      {
        ActionEvent actionEvent = new ActionEvent(getInstance(), 0, item.key);
        actionListener.actionPerformed(actionEvent);
      }
    });
    return spinner;
  }

  private static JTextField createTextField(final Item item, final ActionListener actionListener)
  {
    int width = getInt(item.itemKey + "Width", 100);

    final JTextField textField = new JTextField();
    textField.setToolTipText(item.toolTip);
    Dimension minimumSize = textField.getMinimumSize();
    textField.setMaximumSize(new Dimension(width, minimumSize.height));
    textField.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        ActionEvent actionEvent = new ActionEvent(getInstance(), 0, item.key);
        actionListener.actionPerformed(actionEvent);
      }
    });
    return textField;
  }

  @SuppressWarnings("unused")
  private static Component initializeAbstractButton(AbstractButton button, Item item, ActionListener actionListener)
  {
    button.setName(item.text);
    button.setText(item.text);
    button.setMnemonic(item.mnemonic);
    button.setActionCommand(item.key);
    button.addActionListener(actionListener);
    if (item.toolTip != null)
    {
      button.setToolTipText(item.toolTip);
    }
    if (item.icon != null)
    {
      button.setIcon(item.icon);
    }
    return button;
  }

  public static Image getImage(String iconKey)
  {
    return getIcon(iconKey).getImage();
  }

  public static ImageIcon getIcon(String iconKey)
  {
    String iconName = get(iconKey);
    return getIconByFileName(iconName);
  }

  private static HashMap<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();

  public static ImageIcon getIconByFileName(String iconName)
  {
    ImageIcon icon = iconCache.get(iconName);
    if (icon == null)
    {
      URL url = getInstance().getClass().getResource("/resources/" + iconName);
      if (url == null)
      {
        throw new RuntimeException("Cannot find resource: " + iconName);
      }
      icon = new ImageIcon(url);
      iconCache.put(iconName, icon);
    }
    return icon;
  }

  public static String format(String templateName, Object... arguments)
  {
    String template = get(templateName);
    String message = MessageFormat.format(template, arguments);
    return message;
  }

  public static synchronized Color getColor(String key, Color defaultColor)
  {
    String colorKey = key + "_Color";
    Color color = (Color)colorCache.get(colorKey);
    if (color == null)
    {
      String value = get(key, null);
      if (value == null)
      {
        color = defaultColor;
      }
      else if (Character.isLetter(value.charAt(0)))
      {
        color = getColor(value);
      }
      else
      {
        String[] components = value.split(",");
        if (components.length == 3)
        {
          int red = Integer.decode(components[0]);
          int green = Integer.decode(components[1]);
          int blue = Integer.decode(components[2]);
          color = new Color(red, green, blue);
        }
        else
        {
          int rgb = Integer.decode(value);
          color = new Color(rgb);
        }
      }
      colorCache.put(colorKey, color);
    }
    return color;
  }

  public static Color getColor(String key)
  {
    Color color = getColor(key, null);
    return color;
  }

  private static class Item
  {
    private String key;
    private String itemKey;
    private String text;
    private int mnemonic;
    private String accelerator;
    private String toolTip;
    private String iconName;
    private String iconNameRollover;
    private String iconNamePressed;
    private String type;
    private KeyStroke keyStroke;
    public Icon icon;
    public Icon iconRollover;
    public Icon iconPressed;

    private Item(String key)
    {
      this.key = key;
      itemKey = "Item." + key + ".";
      text = get(itemKey + "Text", key);
      mnemonic = get(itemKey + "Mnemonic", text).charAt(0);
      accelerator = get(itemKey + "Accelerator", null);
      toolTip = get(itemKey + "ToolTip", null);
      iconName = get(itemKey + "Icon", null);
      iconNameRollover = get(itemKey + "Icon.Rollover", null);
      iconNamePressed = get(itemKey + "Icon.Pressed", null);
      type = get(itemKey + "Type", TYPE_ACTION);
      if (accelerator != null)
      {
        keyStroke = KeyStroke.getKeyStroke(accelerator);
      }
      if (iconName != null)
      {
        icon = getIconByFileName(iconName);
      }
      if (iconNameRollover != null)
      {
        iconRollover = getIconByFileName(iconNameRollover);
      }
      if (iconNamePressed != null)
      {
        iconPressed = getIconByFileName(iconNamePressed);
      }
    }
  }

  public static class ItemAction extends AbstractAction implements Action
  {
    private ActionListener actionListener;

    public ItemAction(Item item, ActionListener actionListener)
    {
      putValue(NAME, item.text);
      putValue(ACCELERATOR_KEY, item.keyStroke);
      putValue(ACTION_COMMAND_KEY, item.key);
      putValue(SMALL_ICON, item.icon);
      putValue(SHORT_DESCRIPTION, item.toolTip);
      putValue(MNEMONIC_KEY, item.mnemonic);
      putValue(SELECTED_KEY, Boolean.FALSE); // Components that honor this property only use the value if it is non-null (see Action JavaDoc for more info)
      this.actionListener = actionListener;
    }

    public void actionPerformed(ActionEvent e)
    {
      actionListener.actionPerformed(e);
    }

  }

  public static Component find(Container container, String target)
  {
    int componentCount = container.getComponentCount();
    for (int i = 0; i < componentCount; i++)
    {
      Component component = container.getComponent(i);
      String name = component.getName();
      if (name != null && name.equals(target))
      {
        return component;
      }
      if (component instanceof Container)
      {
        Component childComponent = find((Container)component, target);
        if (childComponent != null)
        {
          return childComponent;
        }
      }
    }
    return null;
  }

  public static URI getUri(String uriKey)
  {
    String uriName = get(uriKey);
    URI uri = Helper.getUri(uriName);
    return uri;
  }

}
