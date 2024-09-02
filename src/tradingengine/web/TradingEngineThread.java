package tradingengine.web;

import javax.naming.NamingException;

import tradingengine.model.TradingEngine.Listener;
import tradingengine.model.TradingEngine.MarketPrice;
import tradingengine.model.TradingEngine.VolumeRecord;

/**
 * a simple delegate which caches buyers and sellers, just like the node.js
 * child processes do.
 */
public class TradingEngineThread extends Thread {

    private static int ID = 0;

    private final TradingEngine engine;

    public TradingEngineThread(long delay, long timeout, Listener listener)
	    throws NamingException {
	super("engine-" + ID++);
	engine = new TradingEngine(delay, timeout, listener);
    }

    @Override
    public void run() {
	engine.run();
    }

    public PurchaseOrder addPurchaseOrder(String who, String productId,
	    int quantity, int id) {

	return engine.addPurchaseOrder(who, productId, quantity, id);
    }

    public SalesOrder addSalesOrder(String who, String productId, int quantity,
	    double price, int id) {
	return engine.addSalesOrder(who, productId, quantity, price, id);
    }

    public VolumeRecord getCurrentVolume(String productId) {
	return engine.getCurrentVolume(productId);
    }

    public MarketPrice getMarketPrice(String productId) {
	return engine.getCurrentMarketPrice(productId);
    }
}
