package owlfroggy.terracottaclient.gameinterface;

import owlfroggy.terracottaclient.DFState;

public interface ModeChangeReceiver {
    public void onModeChanged(DFState.Mode newMode);
}
