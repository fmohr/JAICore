package ai.libs.jaicore.processes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.WinUtils;

/**
 * The process util provides convenient methods for securely killing processes of the operating system. For instance, this is useful whenever sub-processes are spawned and shall be killed reliably.
 *
 * @author fmohr, mwever
 *
 */
public class ProcessUtil {

	/* Logging */
	private static final Logger logger = LoggerFactory.getLogger(ProcessUtil.class);

	private ProcessUtil() {
		/* Intentionally left blank, just prevent an instantiation of this class. */
	}

	/**
	 * Retrieves the type of operating system.
	 *
	 * @return Returns the name of the operating system.
	 */
	public static EOperatingSystem getOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.indexOf("windows") > -1) {
			return EOperatingSystem.WIN;
		}
		if (osName.indexOf("linux") > -1) {
			return EOperatingSystem.LINUX;
		}
		if (osName.contains("mac")) {
			return EOperatingSystem.MAC;
		}
		throw new UnsupportedOperationException("Cannot detect operating system " + osName);
	}

	/**
	 * Gets the OS process for the process list.
	 *
	 * @return The process for the process list.
	 * @throws IOException Thrown if a problem occurred while trying to access the process list process.
	 */
	public static Process getProcessListProcess() throws IOException {
		EOperatingSystem os = getOS();
		switch (os) {
		case WIN:
			return Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
		case LINUX:
			return Runtime.getRuntime().exec("ps -e -o user,pid,ppid,c,size,cmd");
		default:
			throw new UnsupportedOperationException("No action defined for OS " + os);
		}
	}

	/**
	 * Gets a list of running java processes.
	 *
	 * @return The list of running Java processes.
	 * @throws IOException Throwsn if there was an issue accessing the OS's process list.
	 */
	public static Collection<ProcessInfo> getRunningJavaProcesses() throws IOException {
		return new ProcessList().stream().filter(pd -> pd.getDescr().startsWith("java")).collect(Collectors.toList());
	}

	/**
	 * Gets the operating system's process id of the given process.
	 *
	 * @param process The process for which the process id shall be looked up.
	 * @return The process id of the given process.
	 * @throws ProcessIDNotRetrievableException Thrown if the process id cannot be retrieved.
	 */
	public static int getPID(final Process process) throws ProcessIDNotRetrievableException {
		Integer pid;
		try {
			if (getOS() == EOperatingSystem.LINUX || getOS() == EOperatingSystem.MAC) {
				/* get the PID on unix/linux systems */
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = f.getInt(process);
				return pid;

			} else if (getOS() == EOperatingSystem.WIN) {/* determine the pid on windows plattforms */
				return WinUtils.getWindowsProcessId(process).intValue();
			}
		} catch (Exception e) {
			throw new ProcessIDNotRetrievableException("Could not retrieve process ID", e);
		}
		throw new UnsupportedOperationException();
	}

	/**
	 * Kills the process with the given process id.
	 *
	 * @param pid The id of the process which is to be killed.
	 * @throws IOException Thrown if the system command could not be issued.
	 */
	public static void killProcess(final int pid) throws IOException {
		Runtime rt = Runtime.getRuntime();
		if (getOS() == EOperatingSystem.WIN) {
			rt.exec("taskkill /F /PID " + pid);
		} else if (getOS() == EOperatingSystem.MAC) {
			// -2 means keyboard interrupt as hitting ctrl+c in terminal
			rt.exec("kill -2 " + pid);
		} else {
			rt.exec("kill -9 " + pid);
		}
	}

	/**
	 * Kills the provided process with a operating system's kill command.
	 *
	 * @param process The process to be killed.
	 * @throws IOException Thrown if the system command could not be issued.
	 */
	public static void killProcess(final Process process) throws IOException {
		try {
			killProcess(getPID(process));
		} catch (ProcessIDNotRetrievableException e) {
			logger.warn("Cannot kill process with certainty. Thus try to kill the process via the process' destroy method.", e);
			process.destroyForcibly();
		}
	}
}
