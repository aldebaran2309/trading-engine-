package tradingengine.web;

import static tradingengine.web.Constants.DELAY;
import static tradingengine.web.Constants.ID;
import static tradingengine.web.Constants.NUM_KIDS;
import static tradingengine.web.Constants.PRODUCT_IDS;
import static tradingengine.web.Constants.TIMEOUT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tradingengine.model.PurchaseOrder;
import tradingengine.model.Sale;
import tradingengine.model.SalesOrder;
import tradingengine.model.TradingEngine.EventType;
import tradingengine.model.TradingEngine.VolumeRecord;
import tradingengine.model.TradingEngineThread;

@WebServlet(urlPatterns = { "/sell", "/buy", "/result" })
public class TradingEngineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager
	    .getLogger("tradingEngineServlet");

    private static Stats stats = new Stats();
    private static final Map<String, TradingEngineThread> kids = new HashMap<>();
    private static final Map<String, Result> results = new ConcurrentHashMap<>();
    private static final Set<String> knownProducts = Collections
	    .synchronizedSet(new HashSet<>());
    private static final AtomicInteger timedoutSales = new AtomicInteger(0);

    static {
	try {
	    int chunk = PRODUCT_IDS.length / NUM_KIDS;
	    for (int i = 0, j = PRODUCT_IDS.length; i < j; i += chunk) {
		String[] temparray = Arrays.copyOfRange(PRODUCT_IDS, i, i
			+ chunk);
		LOGGER.info("created engine for products " + temparray);
		TradingEngineThread engineThread = new TradingEngineThread(
			DELAY, TIMEOUT, (type, data) -> event(type, data));
		for (int k = 0; k < temparray.length; k++) {
		    LOGGER.debug("mapping productId '" + temparray[k]
			    + "' to engine " + i);
		    kids.put(temparray[k], engineThread);
		}
		LOGGER.info("---started trading");
		engineThread.start();
	    }
	} catch (NamingException e) {
	    LOGGER.error("failed to start engine", e);
	}

	// remove results older than a minute, every 5 seconds.
	// in a real system you wouldnt necessarily cache results like
	// we are doing - the sales are actually persisted by the
	// trading engine - so clients could go look there!
	new Timer("cleaner", true).scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
		LOGGER.error("cleaning results... sales per minute: "
			+ stats.totalSalesPerMinute + ", "
			+ timedoutSales.get() + " timedout orders");
		long now = System.currentTimeMillis();
		List<String> toRemove = new ArrayList<>();
		results.forEach((k, v) -> {
		    if (now - v.created > 60000) {
			toRemove.add(k);
		    }
		});
		toRemove.forEach(k -> results.remove(k));
		LOGGER.info("completed cleaning results in "
			+ (System.currentTimeMillis() - now) + "ms");
	    }
	}, 5000L, 5000L);
    }

    public static synchronized void event(final EventType type,
	    final Object data) {
	switch (type) {
	case SALE: {
	    Sale sale = (Sale) data;
	    int id = sale.getSalesOrder().getId();
	    results.put(String.valueOf(id), new Result(String.valueOf(data)));
	    if (sale.getSalesOrder().getRemainingQuantity() == 0) {
		String msg = "COMPLETED sales order";
		LOGGER.info("\n" + id + ") " + msg + " " + data);
	    } else {
		LOGGER.info("\n" + id + ") PARTIAL sales order " + data);
	    }
	    break;
	}
	case PURCHASE: {
	    Sale sale = (Sale) data;
	    int id = sale.getPurchaseOrder().getId();
	    results.put(String.valueOf(id), new Result(String.valueOf(data)));
	    if (sale.getPurchaseOrder().getRemainingQuantity() == 0) {
		String msg = "COMPLETED purchase order";
		LOGGER.info("\n" + id + ") " + msg + " " + data);
	    } else {
		LOGGER.info("\n" + id + ") PARTIAL purchase order " + data);
	    }
	    break;
	}
	case TIMEOUT_SALESORDER: {
	    timedoutSales.incrementAndGet();
	    SalesOrder so = (SalesOrder) data;
	    String msg = "TIMEOUT sales order";
	    LOGGER.info("\n" + so.getId() + ") " + msg + " " + data);
	    break;
	}
	case TIMEOUT_PURCHASEORDER: {
	    timedoutSales.incrementAndGet();
	    PurchaseOrder po = (PurchaseOrder) data;
	    String msg = "TIMEOUT purchase order";
	    LOGGER.info("\n" + po.getId() + ") " + msg + " " + data);
	    break;
	}
	case STATS: {
	    synchronized (knownProducts) {
		Map<String, List<VolumeRecord>> mapOfVolumeRecords = (Map<String, List<VolumeRecord>>) ((Object[]) data)[2];
		stats.totalSalesPerMinute = knownProducts
			.stream()
			.map(productId -> {
			    return VolumeRecord.aggregate(mapOfVolumeRecords
				    .getOrDefault(productId,
					    Collections.emptyList())).count;
			}).reduce(Integer::sum).orElse(0) * 6; // since stats
							       // are
		// recorded
		// for the last
		// 10 secs
	    }
	    break;
	}
	default:
	    break;
	}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {

	String path = req.getServletPath();
	LOGGER.debug("received command: '" + path + "'");

	String who = req.getParameter("userId");
	String productId = req.getParameter("productId");
	int quantity = Integer.parseInt(req.getParameter("quantity"));
	TradingEngineThread engine = kids.get(productId);
	knownProducts.add(productId);
	int id = ID.getAndIncrement();

	// /buy?productId=1&quantity=10&userId=ant
	if (path.equals("/buy")) {
	    PurchaseOrder po = engine.addPurchaseOrder(who, productId,
		    quantity, id);

	    resp.getWriter().write("\"id\":" + id + ", " + String.valueOf(po));
	} else if (path.equals("/sell")) {
	    double price = Double.parseDouble(req.getParameter("price"));
	    SalesOrder so = engine.addSalesOrder(who, productId, quantity,
		    price, id);

	    resp.getWriter().write("\"id\":" + id + ", " + String.valueOf(so));
	} else if (path.equals("/result")) {
	    String key = req.getParameter("id");
	    Result r = results.get(key);
	    if (r != null) {
		results.remove(key);
		resp.getWriter().write(r.data);
	    } else {
		resp.getWriter().write("UNKNOWN OR PENDING");
	    }
	} else {
	    String msg = "Unknown command " + path;
	    LOGGER.warn(msg);
	}

    }

    private static class Stats {
	int totalSalesPerMinute;
    }

    private static class Result {
	String data;
	long created;

	Result(String data) {
	    this.data = data;
	    this.created = System.currentTimeMillis();
	}
    }
}
