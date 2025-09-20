package owlfroggy.terracottaclient.gameinterface;

import owlfroggy.terracottaclient.DFState;

public interface PlotChangeReceiver {
    void onPlotChanged(int plotId, DFState.Mode mode);
}
