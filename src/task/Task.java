package task;

import java.time.LocalDateTime;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.tinylog.Logger;

public abstract class Task extends RecursiveAction implements Comparable<Task> {

	private static final AtomicLong counter = new AtomicLong(0);

	protected Ctx ctx;
	private volatile task.State state = task.State.INITIAL;
	private volatile boolean softCanceled = false;
	public final long cnt = counter.getAndIncrement();
	public final long tstart = System.currentTimeMillis();
	public final LocalDateTime startDateTime = LocalDateTime.now();
	public long tend = -1;
	private volatile String message = "";
	private BoundedLog boundedLog = new BoundedLog(1000);
	private TaskResult[] results = null;

	public final void setCtxAndInit(Ctx ctx) throws Exception {
		this.ctx = ctx;
		try {
			init();
			state = task.State.INITIAL;
		} catch (Exception e) {
			state = task.State.ERROR;
			throw e;
		}
	}

	protected void init() throws Exception {
	}

	protected abstract void run() throws Exception;

	@Override
	protected final void compute() {		
		try {
			state = task.State.RUNNING;
			run();
			state = task.State.DONE;
		} catch (Exception e) {
			state = task.State.ERROR;
			message = e.getMessage();
			Logger.warn(e);
		} finally {
			tend = System.currentTimeMillis();
		}
	}

	@Override
	public int compareTo(Task o) {
		return Long.compare(this.cnt, o.cnt);
	}
	
	public Ctx geCtx() {
		return ctx;
	}

	public task.State getState() {
		return state;
	}
	
	public void setMessage(String message) {
		this.message = message;
		boundedLog.add(message);
	}
	
	public String getMessage() {
		return message;
	}
	
	public void foreachLog(Consumer<String> consumer) {
		boundedLog.foreach(consumer);	
	}
	
	public long getCurrentTimeOrEnd() {
		return tend < 0 ? System.currentTimeMillis() : tend;
	}
	
	public long getRuntime() {
		return getCurrentTimeOrEnd() - tstart;
	}

	public String getRuntimeText() {
		long tdiff = getRuntime();
		//Duration duration = Duration.ofMillis(tdiff);
		//return duration.toString();
		long seconds = tdiff / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long secondsPart = seconds % 60;
		long minutesPart = minutes % 60;
		String s = hours + ":" +  String.format("%02d", minutesPart) + ":" + String.format("%02d", secondsPart);
		return s;
	}
	
	public void softCancel() {
		softCanceled = true;
	}

	public boolean isSoftCanceled() {
		return softCanceled;
	}

	public boolean isCancelable() {
		return this.getClass().isAnnotationPresent(Cancelable.class);
	}
	
	public void setResult(TaskResult ...results) {
		this.results = results;
	}

	public boolean hasResults() {
		TaskResult[] r = results;
		return r != null && r.length > 0;
	}
	
	public void visitResults(TaskResult.Visitor visitor) {
		visitor.visit(results);
	}

	public TaskResult getResult(int resultNumber) {
		TaskResult[] rs = results;
		if(rs == null || resultNumber < 0 || rs.length <= resultNumber) {
			throw new RuntimeException("no result with number: " + resultNumber);
		}
		TaskResult r = rs[resultNumber];
		if(r == null) {
			throw new RuntimeException("no result with number: " + resultNumber);
		}
		return r;
	}
}
