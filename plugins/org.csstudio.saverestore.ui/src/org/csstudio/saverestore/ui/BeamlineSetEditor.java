package org.csstudio.saverestore.ui;

import static org.csstudio.ui.fx.util.FXUtilities.setGridConstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.data.BeamlineSetData;
import org.csstudio.ui.fx.util.FXEditorPart;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;

/**
 *
 * <code>BeamlineSetEditor</code> is an implementation of the {@link EditorPart} which allows editing the beamline sets.
 * User is allowed to change the description and the list of pvs in the set.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class BeamlineSetEditor extends FXEditorPart {

    public static final String ID = "org.csstudio.saverestore.ui.editor.beamlineseteditor";

    private Scene scene;
    private BorderPane contentPane;
    private TextArea descriptionArea;
    private TextArea contentArea;

    private BeamlineSetData data;
    private String orgText;

    private boolean dirty = false;
    private BeamlineSetController controller;

    /**
     * Constructs a new beamline set editor.
     */
    public BeamlineSetEditor() {
        this.controller = new BeamlineSetController(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "org.csstudio.saverestore.ui.help.beamlineseteditor");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        if (data.getStoredDate() == null) {
            doSaveAs();
        } else {
            monitor.beginTask("Save beamline set", 1);
            final BeamlineSetData data = createData();
            if (data.getDescription().equals(this.data.getDescription())
                && data.getPVList().equals(this.data.getPVList())) {
                MessageDialog.openInformation(getSite().getShell(), "Save Beamline Set",
                    "Theare are no changes between the saved and this beamline set.");
                return;
            }

            SaveRestoreService.getInstance().execute("Save Beamline Set", () -> {
                final Optional<BeamlineSetData> ds = controller.save(data);
                getSite().getShell().getDisplay().asyncExec(() -> {
                    monitor.done();
                    ds.ifPresent(e -> setInput(new BeamlineSetEditorInput(e)));
                });
            });
        }
    }

    private BeamlineSetData createData() {
        String description = descriptionArea.getText().trim();
        String text = contentArea.getText();
        text = text.replace(" ", "\n");
        String[] pvs = text.split("\\n");
        List<String> pvList = new ArrayList<>(pvs.length);
        for (String s : pvs) {
            pvList.add(s);
        }
        return new BeamlineSetData(data.getDescriptor(), pvList, description);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        BeamlineSetData data = createData();
        if (data.getDescription().equals(this.data.getDescription())
            && data.getPVList().equals(this.data.getPVList())) {
            boolean ans = MessageDialog.openQuestion(getSite().getShell(), "Save Beamline Set As",
                "Theare are no changes between the saved and this beamline set. Are you sure you want to save it as a new beamline set?");
            if (!ans) {
                return;
            }
        }
        new RepositoryTreeBrowser(this, data.getDescriptor()).openAndWait()
            .ifPresent(beamlineSet -> SaveRestoreService.getInstance().execute("Save Beamline Set", () -> {
                controller.save(new BeamlineSetData(beamlineSet, data.getPVList(), data.getDescription())).ifPresent(
                    d -> getSite().getShell().getDisplay().asyncExec(() -> setInput(new BeamlineSetEditorInput(d))));
            }));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return dirty;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
     */
    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        setPartName(input.getName());
        BeamlineSetData data = input.getAdapter(BeamlineSetData.class);
        if (data != null) {
            SaveRestoreService.getInstance().execute("Open beamline set", () -> setBeamlineSet(data));
        }
        firePropertyChange(PROP_INPUT);
        firePropertyChange(PROP_TITLE);
    }

    private void setBeamlineSet(final BeamlineSetData data) {
        this.data = data;
        List<String> list = data.getPVList();
        final StringBuilder sb = new StringBuilder(list.size() * 40);
        list.forEach(e -> sb.append(e).append('\n'));
        Platform.runLater(() -> {
            String description = data.getDescription();
            descriptionArea.setText(description);
            orgText = sb.toString();
            contentArea.setText(orgText);
            dirty = false;
            firePropertyChange(PROP_DIRTY);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#createFxScene()
     */
    @Override
    protected Scene createFxScene() {
        contentPane = new BorderPane();
        contentPane.setCenter(createCenterPane());
        scene = new Scene(contentPane);
        return scene;
    }

    private Node createCenterPane() {
        GridPane grid = new GridPane();
        grid.setVgap(3);
        grid.setPadding(new Insets(5, 5, 5, 5));
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.setFont(Font.font(15));
        descriptionArea = new TextArea();
        descriptionArea.setEditable(true);
        descriptionArea.setTooltip(new Tooltip("Brief description of this beamline set"));
        descriptionArea.setMaxWidth(Double.MAX_VALUE);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(4);
        descriptionArea.textProperty().addListener((a, o, n) -> {
            dirty = true;
            firePropertyChange(PROP_DIRTY);
        });

        Label contentLabel = new Label("PV List:");
        contentLabel.setFont(Font.font(15));
        contentArea = new TextArea();
        contentArea.setEditable(true);
        contentArea.setTooltip(new Tooltip("The list of PVs in this beamline set"));
        contentArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentArea.setWrapText(false);
        contentArea.textProperty().addListener((a, o, n) -> {
            dirty = true;
            firePropertyChange(PROP_DIRTY);
        });

        setGridConstraints(descriptionLabel, true, true, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        setGridConstraints(contentLabel, true, true, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
        setGridConstraints(descriptionArea, true, true, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER);
        setGridConstraints(contentArea, true, true, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);

        grid.add(descriptionLabel, 0, 0);
        grid.add(descriptionArea, 0, 1);
        grid.add(contentLabel, 0, 2);
        grid.add(contentArea, 0, 3);

        return grid;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.ui.util.FXEditorPart#setFxFocus()
     */
    @Override
    public void setFxFocus() {
        if (contentArea != null) {
            contentArea.requestFocus();
        }
    }
}
