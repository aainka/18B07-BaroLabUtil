package com.barolab.sync;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;

import com.barolab.log.LogConfig;

import lombok.extern.java.Log;

@Log
public class Sync {

//	String remote_homeDir = "/proj7/GITHUB/18004-DashConsole/";

//	String remote_homeDir2 = "/proj7java-workspace/18B07-BaroLabUtil/";
	// RemoteFileScanner remote = new RemoteFileScanner(remote_host, "/proj7");
//	LocalFileScanner local = new LocalFileScanner("xxx");
	// RemoteFileScanner remote = new RemoteFileScanner(host13F,
	// "/root/project");
//	RemoteFileScanner remote = new RemoteFileScanner(hostFun25, "/proj7/GITHUB/18B07-BaroLabUtil/");
	// LocalFileScanner local = new LocalFileScanner("S:/tmp/18B07-BaroLabUtil");
//	
//
//	RemoteFileScanner remote = new RemoteFileScanner("192.168.25.50:9292", "/root/AAA/18B07-BaroLabUtil");

	boolean syncGetLock = false;
	boolean syncPutLock = true;
	LocalFileApi local;
	RemoteFileApi remote;

	String hostHomeOne = "110.13.71.93:9292";
	String host13F = "100.99.14.164:9292";
	String hostFun25 = "211.239.124.246:19808";
	String hostLocal = "192.168.25.50:9292";

	StringBuilder stringReport = new StringBuilder();
	List<OV_ScanOp> scanList = null;

	private void config_one() {
		local = new LocalFileApi("C:/@SWDevelopment/workspace-java/18B07-BaroLabUtil/");
		remote = new RemoteFileApi(hostHomeOne, "/root/SynHub/18B07-BaroLabUtil");
//		local = new LocalFileScanner("C:/@SWDevelopment/workspace-java/18004-DashConsole");
//		remote = new RemoteFileScanner(hostHomeOne, "/root/project/18004-DashConsole");
	}

	public DefaultTableModel getTableModel() {
		LogConfig.setLevel("com.barolab.sync", Level.INFO);
		System.out.println("testGui.... Called");
		scanList = new LinkedList<OV_ScanOp>();
		String host;
		String localDir;
		if (true) {
			syncGetLock = true;
			syncPutLock = true;
			host = hostLocal;
			localDir = "S:/sw-dev/eclipse-workspace-18b";
		} else {
			syncGetLock = true;
			syncPutLock = true;
			host = hostHomeOne;
			localDir = "C:/@SWDevelopment/workspace-java";
		}
		
		// LogConfig.setLevel("com.barolab.sync.*", Level.ALL);
		syncProject("18B07-BaroLabUtil", host, "/root/SynHub", localDir);
//		syncProject("19A01-PyRestfulApi", host, "/root/SynHub", localDir);
//		syncProject("18004-DashConsole", host, "/root/SynHub", localDir);

		String[] columnName = { "File", "Mode", "Date" };
		DefaultTableModel model = new DefaultTableModel(columnName, 0);

		for (OV_ScanOp trace : scanList) {
			Object[] objList = new Object[] { trace.getSrc().getPath(), trace.getOp(), trace.getSrc().getUpdated() };
			model.addRow(objList);
		}
		// JYWidget.find("syncTalbe").setData(model);
		return model;
	}

	public void test() {
		String host;
		String localDir;
		if (true) {
			syncGetLock = true;
			syncPutLock = true;
			host = hostLocal;
			localDir = "S:/sw-dev/eclipse-workspace-18b";
		} else {
			syncGetLock = true;
			syncPutLock = true;
			host = hostHomeOne;
			localDir = "C:/@SWDevelopment/workspace-java";
		}
		// LogConfig.setLevel("com.barolab.sync.*", Level.ALL);
		syncProject("18B07-BaroLabUtil", host, "/root/SynHub", localDir);
//		syncProject("19A01-PyRestfulApi", host, "/root/SynHub", localDir);
//		syncProject("18004-DashConsole", host, "/root/SynHub", localDir);

	}

	public void syncProject(String projName, String host, String remoteDir, String localDir) {
		System.out.println("Project=" + projName);
		stringReport = new StringBuilder();
		local = new LocalFileApi(localDir + "/" + projName);
		remote = new RemoteFileApi(host, remoteDir + "/" + projName);
		// config_one();
		OV_FileInfo a = local.scanAll();
		OV_FileInfo b = remote.scanAll();
		compareFile(a, b);
		System.out.println("Completed");
		System.out.println(stringReport.toString());
	}

	/**
	 * Directory time must be changed after all node changed under own directory.
	 * 
	 * @param srcParent
	 * @param dstParent
	 */
	private void compareFile(OV_FileInfo srcParent, OV_FileInfo dstParent) {
		if (dstParent == null) {
			return; // when lock
		}
		if (srcParent.children != null) {
			for (OV_FileInfo src : srcParent.children) {
				OV_FileInfo dst = find(src, dstParent.children);
				if (dst == null) {
					report("--> X " + src.getFullPath());
					{
						src.read();
						dst = new OV_FileInfo(src.getPath(), dstParent);
						dst.copyFrom(src);
						OV_ScanOp scanRpt = OV_ScanOp.create("RemoteCreate", scanList, src, dst);
						if (!syncPutLock) {
							dst = scanRpt.remotePut();
							if (dst != null) {
								srcParent.setChildChanged(true); // @Bug
							} // recursive
						}
					}
				} else {
					if (!isContextSame(src, dst)) {
						compareTime(src, dst);
					}
				}
			//	 isContextSame(src, dst);
				compareFile(src, dst); // recursive
			}
			if (srcParent.is_dir() && srcParent.isChildChanged()) {
				OV_ScanOp scanRpt = OV_ScanOp.create("Dir Update", scanList, srcParent, dstParent);
				// remotePut(srcParent, dstParent.getScanner()); // overwrite time
				scanRpt.remotePut();
			}
			if (dstParent.is_dir() && dstParent.isChildChanged()) {
				// localUpdateTime(dstParent, srcParent.getScanner());
			}
		}
	}

	private boolean isContextSame(OV_FileInfo src, OV_FileInfo dst) {
		if (dst == null) {
			return false; // remotewrite lock 시 null.
		}
		if (src.is_dir()) {
			return true;
		}
		src.read();
		dst.read();
		String src_context = src.getText_in_file();
		String dst_context = dst.getText_in_file();
		int diff = src_context.length() - dst_context.length();
		if (src_context.equals(dst_context)) {
			// log.severe("*************** Context is same " + src.getFullPath());
			return true;
		} else {
			log.severe("*************** Context is different " + src.getFullPath());
			return false;
		}

	}

	private OV_FileInfo find(OV_FileInfo t, LinkedList<OV_FileInfo> children) {
		if (children != null) {
			String name = t.getShortName();
			for (OV_FileInfo c : children) {
				if (name.equals(c.getShortName())) {
					// System.out.println("find " + name + " c1=" + c.getShortName());
					return c;
				}
			}
		}
		return null;
	}

	private void compareTime(OV_FileInfo src, OV_FileInfo dst) {
		long s0 = src.getUpdated().getTime();
		long d0 = dst.getUpdated().getTime();
		long diff = d0 - s0;
		if (diff != 0) {
			if (diff < 0) {
				report("--> " + src.getFullPath() + ", t=" + src.getUpdated());
				log.info("--> " + src.getFullPath() + ", t=" + diff);
				{
					src.read();
					OV_ScanOp scanRpt = OV_ScanOp.create("RemotePut", scanList, src, dst);
					if (!syncPutLock) {
						dst = scanRpt.remotePut();
						if (dst != null) {
							src.getParent().setChildChanged(true); // @Bug
						} // recursive
					}
				}
			} else {
				report("<-- " + src.getFullPath() + ", s=" + src.getUpdated() + " d=" + dst.getUpdated());
				if (!dst.is_dir()) {
					dst.read();
				}
				OV_ScanOp rpt = OV_ScanOp.create("RemoteGet", scanList, src, dst);
				if (!syncGetLock) {
					rpt.remoteGet();
				}
			}
		}
	}

	// ##################################################################
	// ## Remote Sync
	// ##################################################################

	public void updateSelectedSync(int[] rownums) {
		for (int rownum : rownums) {
			OV_ScanOp rpt = scanList.get(rownum);
			if (rpt.getOp().equals("RemoteGet")) {
				rpt.remoteGet();
			}
			if (rpt.getOp().equals("RemotePut")) {
				rpt.remotePut();
			}
			if (rpt.getOp().equals("RemoteCreate")) {
				rpt.remotePut();
			}
		}

	}

	private void report(String msg) {
		stringReport.append(msg + "\n");
	}

	public static void main9(String[] args) {
		new Sync().test();

	}

}
