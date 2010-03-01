package org.csstudio.opibuilder.preferences;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.platform.ui.swt.stringtable.RowEditDialog;
import org.csstudio.platform.ui.swt.stringtable.StringTableEditor;
import org.csstudio.platform.util.StringUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**Field Editor for String table (a 2D String Array) input.
 * @author Xihui Chen
 *
 */
public class StringTableFieldEditor extends FieldEditor {
	

	private static final String DECODE_ERROR_MESSAGE = "Failed to decode string table. No quotes are allowed in string table.\n";

	private static final char ROW_SEPARATOR = '|'; //$NON-NLS-1$

	private static final char ITEM_SEPARATOR = ','; //$NON-NLS-1$

	private static final char QUOTE = '\"'; //$NON-NLS-1$

	private StringTableEditor tableEditor;
	
	private String[] headers;
	private boolean[] editable;
	private List<String[]> items;
	private int[] columnsMinWidth;
	private RowEditDialog rowEditDialog;
	
	protected StringTableFieldEditor() {
	}
 	
	/** Creates an editable table.  The size of headers array implies the number of columns. 
	 * @param parent The composite which the table resides in
	 * @param headers Contains the header for each column
	 * @param editable Whether it is editable for each column. The size must be same as headers.  
	 */
	public StringTableFieldEditor(String name, String labelText, final Composite parent, final String[] headers, 
			final boolean[] editable, final RowEditDialog rowEditDialog,
			final int[] columnsMinWidth) {
		init(name, labelText);
		this.headers = headers;
		this.editable = editable;
		this.columnsMinWidth = columnsMinWidth;
		this.rowEditDialog = rowEditDialog;		
		this.items = new ArrayList<String[]>();		
        createControl(parent);
		
	}
	
	@Override
	protected void adjustForNumColumns(int numColumns) {
		GridData gd = (GridData)tableEditor.getLayoutData();
		gd.horizontalSpan = numColumns;
		gd = (GridData)getLabelControl().getLayoutData();
		gd.horizontalSpan = numColumns;
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = numColumns;		
		getLabelControl().setLayoutData(gd);
		tableEditor = new StringTableEditor(
				parent, headers, editable, items, rowEditDialog, columnsMinWidth);
		gd = new GridData();
		gd.horizontalSpan = numColumns;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		gd.verticalAlignment = SWT.FILL;
		tableEditor.setLayoutData(gd);
		
	}

	@Override
	protected void doLoad() {
		if(tableEditor != null){
			try {
				items = decodeStringTable(getPreferenceStore().getString(getPreferenceName()));
				tableEditor.updateInput(items);
			} catch (Exception e) {
				MessageDialog.openError(getPage().getShell(), "Error", 
						DECODE_ERROR_MESSAGE + e.getMessage());
				CentralLogger.getInstance().error(this, e);
			}
		}
	}

	@Override
	protected void doLoadDefault() {
		if(tableEditor != null){
			try {
				items = decodeStringTable(getPreferenceStore().getDefaultString(getPreferenceName()));
				tableEditor.updateInput(items);				
			} catch (Exception e) {
				MessageDialog.openError(getPage().getShell(), "Error", 
						DECODE_ERROR_MESSAGE + e.getMessage());
				CentralLogger.getInstance().error(this, e);
			}
		}
	}

	@Override
	protected void doStore() {
		getPreferenceStore().setValue(getPreferenceName(), flattenStringTable(items));
	}

	@Override
	public int getNumberOfControls() {
		return 1;
	}
	
	/**Flatten a string table to a single line string.
	 * @param stringTable
	 * @return
	 */
	public static String flattenStringTable(List<String[]> stringTable){
		StringBuilder result = new StringBuilder(""); //$NON-NLS-1$
		for(String[] row : stringTable){
			for(String item : row){
				result.append(QUOTE + item + QUOTE + ITEM_SEPARATOR);				
			}
			if(row.length > 0)
				result.deleteCharAt(result.length()-1);
			result.append(ROW_SEPARATOR);			
		}
		if(stringTable.size() > 0)
				result.deleteCharAt(result.length()-1);
		return result.toString();
	}
	
	public static List<String[]> decodeStringTable(final String flattedString) throws Exception{
		final List<String[]> result = new ArrayList<String[]>();
		final String[] rows = StringUtil.splitIgnoreInQuotes(flattedString, ROW_SEPARATOR, false); 
		for(String rowString : rows){
		    // Skip empty rowString, don't split it into String[1] { "" }
		    if (rowString.length() <= 0)
		        continue;
			final String[] items = StringUtil.splitIgnoreInQuotes(rowString, ITEM_SEPARATOR, true);
			result.add(items);
		}
		return result;
	}

}
