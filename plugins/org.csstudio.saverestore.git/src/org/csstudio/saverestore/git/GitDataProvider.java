package org.csstudio.saverestore.git;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.csstudio.saverestore.CompletionNotifier;
import org.csstudio.saverestore.DataProvider;
import org.csstudio.saverestore.DataProviderException;
import org.csstudio.saverestore.SaveRestoreService;
import org.csstudio.saverestore.SearchCriterion;
import org.csstudio.saverestore.data.BaseLevel;
import org.csstudio.saverestore.data.BeamlineSet;
import org.csstudio.saverestore.data.BeamlineSetData;
import org.csstudio.saverestore.data.Branch;
import org.csstudio.saverestore.data.Snapshot;
import org.csstudio.saverestore.data.VSnapshot;
import org.csstudio.saverestore.git.Result.ChangeType;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * <code>GitDataProvider</code> the data provider implementation that uses git repository to store all the data.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class GitDataProvider implements DataProvider {

    public static final String ID = "org.csstudio.saverestore.git.dataprovider";

    private final GitManager grm;
    private List<CompletionNotifier> notifiers;

    /**
     * Constructs a new GitDataProvider.
     */
    public GitDataProvider() {
        notifiers = new ArrayList<>();
        grm = new GitManager();
        SaveRestoreService.getInstance().execute("Git Initialise", () -> {
            try {
                URI remote = Activator.getInstance().getGitURI();
                File dest = Activator.getInstance().getDestination();
                grm.initialise(remote, dest);
            } catch (GitAPIException e) {
                throw new RuntimeException("Could not instantiate git data provider.", e);
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#isReinitSupported()
     */
    @Override
    public boolean isReinitSupported() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#reinitialise()
     */
    @Override
    public boolean reinitialise() throws DataProviderException {
        try {
            URI remote = Activator.getInstance().getGitURI();
            File dest = Activator.getInstance().getDestination();
            GitManager.deleteFolder(dest);
            return grm.initialise(remote, dest);
        } catch (GitAPIException e) {
            throw new DataProviderException("Could not initialise git repository.", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#addCompletionNotifier(org.csstudio.saverestore.CompletionNotifier)
     */
    @Override
    public void addCompletionNotifier(CompletionNotifier notifier) {
        synchronized (notifiers) {
            notifiers.add(notifier);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#removeCompletionNotifier(org.csstudio.saverestore.CompletionNotifier)
     */
    @Override
    public void removeCompletionNotifier(CompletionNotifier notifier) {
        synchronized (notifiers) {
            notifiers.remove(notifier);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getBranches()
     */
    @Override
    public Branch[] getBranches() throws DataProviderException {
        try {
            List<Branch> branches = grm.getBranches();
            return branches.toArray(new Branch[branches.size()]);
        } catch (GitAPIException e) {
            throw new DataProviderException("Error loading the branches list.", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getBaseLevels(org.csstudio.saverestore.Branch)
     */
    @Override
    public BaseLevel[] getBaseLevels(Branch branch) throws DataProviderException {
        try {
            List<BaseLevel> bls = grm.getBaseLevels(branch);
            return bls.toArray(new BaseLevel[bls.size()]);
        } catch (GitAPIException | IOException e) {
            throw new DataProviderException("Could not read the base levels.", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getBeamlineSets(java.util.Optional,
     * org.csstudio.saverestore.data.Branch)
     */
    @Override
    public BeamlineSet[] getBeamlineSets(Optional<BaseLevel> baseLevel, Branch branch) throws DataProviderException {
        try {
            List<BeamlineSet> sets = grm.getBeamlineSets(baseLevel, branch);
            return sets.toArray(new BeamlineSet[sets.size()]);
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException("Error loading the beamline set list.", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getSnapshots(org.csstudio.saverestore.data.BeamlineSet, boolean,
     * java.util.Optional)
     */
    @Override
    public Snapshot[] getSnapshots(BeamlineSet set, boolean all, Optional<Snapshot> fromThisOneBack)
        throws DataProviderException {
        try {
            List<Snapshot> snapshots = grm.getSnapshots(set,
                all ? 0 : SaveRestoreService.getInstance().getNumberOfSnapshots(), fromThisOneBack);
            return snapshots.toArray(new Snapshot[snapshots.size()]);
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException("Error retrieving the snapshots list for '" + set.getPathAsString() + "'.",
                e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getBeamlineSetContent(org.csstudio.saverestore.BeamlineSet)
     */
    @Override
    public BeamlineSetData getBeamlineSetContent(BeamlineSet set) throws DataProviderException {
        try {
            return grm.loadBeamlineSetData(set, Optional.empty());
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException("Error loading the beamline set data for '" + set.getPathAsString() + "'.",
                e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#createNewBranch(org.csstudio.saverestore.Branch, java.lang.String)
     */
    @Override
    public String createNewBranch(Branch originalBranch, String newBranchName) throws DataProviderException {
        Branch branch = null;
        try {
            branch = grm.createBranch(originalBranch, newBranchName);
        } catch (GitAPIException | IOException e) {
            throw new DataProviderException("Error creating branch '" + newBranchName + "'.", e);
        }
        for (CompletionNotifier n : getNotifiers()) {
            n.branchCreated(branch);
        }
        return newBranchName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#saveBeamlineSet(org.csstudio.saverestore.BeamlineSetData,
     * java.lang.String)
     */
    @Override
    public BeamlineSetData saveBeamlineSet(BeamlineSetData set, String comment) throws DataProviderException {
        Result<BeamlineSetData> answer = null;
        try {
            answer = grm.saveBeamlineSet(set, comment);
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException(
                "Error saving beamline set '" + set.getDescriptor().getPathAsString() + "'.", e);
        }
        if (answer.change == ChangeType.PULL) {
            for (CompletionNotifier n : getNotifiers()) {
                n.synchronised();
            }
        } else if (answer.change == ChangeType.SAVE) {
            for (CompletionNotifier n : getNotifiers()) {
                n.beamlineSaved(answer.data);
            }
        }
        return answer.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#deleteBeamlineSet(org.csstudio.saverestore.data.BeamlineSet,
     * java.lang.String)
     */
    @Override
    public boolean deleteBeamlineSet(BeamlineSet set, String comment) throws DataProviderException {
        Result<BeamlineSet> answer = null;
        try {
            answer = grm.deleteBeamlineSet(set, comment);
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException("Error deleting beamline set '" + set.getPathAsString() + "'.", e);
        }
        if (answer.change == ChangeType.PULL) {
            for (CompletionNotifier n : getNotifiers()) {
                n.synchronised();
            }
        } else if (answer.change == ChangeType.SAVE) {
            for (CompletionNotifier n : getNotifiers()) {
                n.beamlineDeleted(answer.data);
            }
        }
        return answer.data != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#saveSnapshot(org.csstudio.saverestore.VSnapshot, java.lang.String)
     */
    @Override
    public VSnapshot saveSnapshot(VSnapshot data, String comment) throws DataProviderException {
        Result<VSnapshot> answer = null;
        try {
            answer = grm.saveSnapshot(data, comment);
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException(
                "Error saving snapshot set for '" + data.getBeamlineSet().getPathAsString() + "'.", e);
        }
        if (answer.change == ChangeType.PULL) {
            for (CompletionNotifier n : getNotifiers()) {
                n.synchronised();
            }
        } else if (answer.change == ChangeType.SAVE) {
            for (CompletionNotifier n : getNotifiers()) {
                n.snapshotSaved(answer.data);
            }
        }
        return answer.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#tagSnapshot(org.csstudio.saverestore.data.Snapshot,
     * java.util.Optional, java.util.Optional)
     */
    @Override
    public Snapshot tagSnapshot(Snapshot snapshot, Optional<String> tagName, Optional<String> tagMessage)
        throws DataProviderException {
        Result<Snapshot> answer = null;
        try {
            answer = grm.tagSnapshot(snapshot, tagName.orElse(null), tagMessage.orElse(null));
        } catch (IOException | GitAPIException e) {
            throw new DataProviderException("Error creating the tag for snapshot '" + snapshot.getDate() + "'.", e);
        }
        if (answer.change == ChangeType.PULL) {
            for (CompletionNotifier n : getNotifiers()) {
                n.synchronised();
            }
        } else if (answer.change == ChangeType.SAVE) {
            for (CompletionNotifier n : getNotifiers()) {
                n.snapshotTagged(answer.data);
            }
        }
        return answer.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#getSnapshotContent(org.csstudio.saverestore.Snapshot)
     */
    @Override
    public VSnapshot getSnapshotContent(Snapshot snapshot) throws DataProviderException {
        try {
            return grm.loadSnapshotData(snapshot);
        } catch (ParseException | IOException | GitAPIException e) {
            throw new DataProviderException("Error loading the snapshot content for snapshot '"
                + snapshot.getBeamlineSet().getPathAsString() + "[" + snapshot.getDate() + "]'.", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#findSnapshots(java.lang.String, org.csstudio.saverestore.data.Branch,
     * java.util.List)
     */
    @Override
    public Snapshot[] findSnapshots(String expression, Branch branch, List<SearchCriterion> criteria)
        throws DataProviderException {
        Set<Snapshot> list = new LinkedHashSet<>();
        boolean sort = false;
        try {
            if (criteria.contains(SearchCriterion.COMMENT)) {
                list.addAll(grm.findSnapshotsByComment(expression, branch));
            }
            int size = list.size();
            if (criteria.contains(SearchCriterion.TAG_MESSAGE) && criteria.contains(SearchCriterion.TAG_NAME)) {
                list.addAll(grm.findSnapshotsByTag(expression, branch));
            } else {
                if (criteria.contains(SearchCriterion.TAG_MESSAGE)) {
                    list.addAll(grm.findSnapshotsByTagMessage(expression, branch));
                } else if (criteria.contains(SearchCriterion.TAG_NAME)) {
                    list.addAll(grm.findSnapshotsByTagName(expression, branch));
                }
            }
            if (size > 0 && list.size() != size) {
                sort = true;
            }
        } catch (GitAPIException | IOException e) {
            throw new DataProviderException("Error search for snapshot that match the expression '" + expression
                + "' using criteria '" + criteria.toString() + ".", e);
        }
        Snapshot[] snapshots = list.toArray(new Snapshot[list.size()]);
        if (sort) {
            //there is a natural order of snapshots provided by GitManager. Sort only if the search was performed more
            //than once.
            Arrays.sort(snapshots);
        }
        return snapshots;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#importData(org.csstudio.saverestore.data.BeamlineSet,
     * org.csstudio.saverestore.data.Branch, java.util.Optional, org.csstudio.saverestore.DataProvider.ImportType)
     */
    @Override
    public boolean importData(BeamlineSet source, Branch toBranch, Optional<BaseLevel> toBaseLevel, ImportType type)
        throws DataProviderException {
        Result<Boolean> answer = null;
        try {
            answer = grm.importData(source, toBranch, toBaseLevel, type);
        } catch (IOException | GitAPIException | ParseException e) {
            throw new DataProviderException("Error importing data from '" + source.getPathAsString() + "'.", e);
        }

        if (answer.change == ChangeType.PULL) {
            for (CompletionNotifier n : getNotifiers()) {
                n.synchronised();
            }
        } else if (answer.change == ChangeType.SAVE) {
            for (CompletionNotifier n : getNotifiers()) {
                n.dataImported(source, toBranch, toBaseLevel);
            }
        }
        return answer.data;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#areBranchesSupported()
     */
    @Override
    public boolean areBranchesSupported() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#areBaseLevelsSupported()
     */
    @Override
    public boolean areBaseLevelsSupported() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.saverestore.DataProvider#synchronise()
     */
    @Override
    public boolean synchronise() throws DataProviderException {
        boolean sync = false;
        try {
            sync = grm.synchronise(Optional.empty());
        } catch (GitAPIException e) {
            throw new DataProviderException("Error synchronising local repository with remote.", e);
        }
        for (CompletionNotifier n : getNotifiers()) {
            n.synchronised();
        }
        return sync;
    }

    private CompletionNotifier[] getNotifiers() {
        CompletionNotifier[] nots = null;
        synchronized (notifiers) {
            nots = notifiers.toArray(new CompletionNotifier[notifiers.size()]);
        }
        return nots;
    }
}
