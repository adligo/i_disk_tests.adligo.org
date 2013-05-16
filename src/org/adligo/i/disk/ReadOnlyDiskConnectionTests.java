package org.adligo.i.disk;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.adligo.i.pool.I_Pool;
import org.adligo.i.pool.Pool;
import org.adligo.i.pool.PoolConfiguration;
import org.adligo.tests.ATest;

public class ReadOnlyDiskConnectionTests extends ATest {
	private I_Pool<ReadOnlyDiskConnection> pool = new Pool<ReadOnlyDiskConnection>(
			new PoolConfiguration<ReadOnlyDiskConnection>("testFactory", new ReadOnlyDiskConnectionFactory(), 1));
	public static String baseDir = BaseDir.getBaseDir("i_disk_tests");
	
	private InputStream in;
	private FileFilter txtFileFilter = new FileFilter() {
		
		@Override
		public boolean accept(File pathname) {
			if (pathname.getName().contains(".txt")) {
				return true;
			}
			return false;
		}
	};
	
		
	public void testSimpleMethods() {
		ReadOnlyDiskConnection con = pool.getConnection();
		con.dispose();
		assertFalse(con.isReadWrite());
	}
	
	public void testCheckIfFileExists() {
		ReadOnlyDiskConnection con = pool.getConnection();
		String message = "running in " + new File(".").getAbsolutePath() +
				"\n base dir is " + baseDir;
		assertFalse(message, con.checkIfFileExists(baseDir + "test_data/foo.txt"));
		assertTrue(message,con.checkIfFileExists(baseDir + "test_data/read/hello.txt"));
		con.returnToPool();
	}
	
	public void testCheckIfDirectoryExists() {
		ReadOnlyDiskConnection con = pool.getConnection();
		assertFalse(con.checkIfDirectoryExists(baseDir + "test_data/foo"));
		assertTrue(con.checkIfDirectoryExists(baseDir + "test_data/read"));
		con.returnToPool();
	}
	
	public void testCheckHidden() {
		ReadOnlyDiskConnection con = pool.getConnection();
		assertFalse(con.checkIfHidden(baseDir + "test_data/foo"));
		assertTrue(con.checkIfHidden(baseDir + "test_data/read/.hidden"));
		con.returnToPool();
	}
	
	
	public void testCheckLastModified() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getModifiedTime(baseDir + "test_data/read/hello.txt");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		Long mod2 = con.getModifiedTime(baseDir + "test_data/read/hello.txt");
		assertEquals(mod, mod2);
		
		mod2 = con.getModifiedTime(baseDir + "notAFile.txt");
		assertNull(mod2);
		con.returnToPool();
	}
	
	public void testGetFreeSpace() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getFreeSpace(baseDir + "test_data/read");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		
		mod = con.getFreeSpace(baseDir + "dirNotThere");
		assertNull(mod);
		con.returnToPool();
	}
	
	public void testUsableSpace() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getUsableSpace(baseDir + "test_data/read");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		
		mod = con.getUsableSpace(baseDir + "dirNotThere");
		assertNull(mod);
		con.returnToPool();
	}
	
	
	
	public void testReadFile() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		final StringBuffer sb = new StringBuffer();
		con.readFile(baseDir + "test_data/read/hello.txt", new I_InputProcessor() {
			
			@Override
			public void process(InputStream p, long byteLength) throws IOException {
				byte [] bytes = new byte[(int) byteLength];
				p.read(bytes);
				sb.append(new String(bytes,"ASCII"));
			}
		});
		assertEquals("hello i_disk", sb.toString());
	}
	
	public void testReadFileExceptoin() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		
		con.readFile(baseDir + "test_data/read/hello.txt", new I_InputProcessor() {
			
			@Override
			public void process(InputStream p, long byteLength) throws IOException {
				in = p;
				throw new IOException("FromTest");
			}
		});
		IOException caught = null;
		try {
			in.read();
		} catch (IOException x) {
			caught = x;
		}
		assertNotNull(caught);
		assertEquals("Stream Closed", caught.getMessage());
	}
	public void testListContents() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", 0);
		assertEquals(3, items.size());
		DiskItem item = items.get(0);
		assertEquals("CVS", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains("test_data"));
		
		item = items.get(1);
		assertEquals("read", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data"));
		
		item = items.get(2);
		assertEquals("write", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data"));
	}
	
	public void testListContentsInfinateRecurse() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", -1);
		assertEquals(18, items.size());
		DiskItem item = items.get(0);
		assertDir(item, "CVS", baseDir + "test_data");
		item = items.get(1);
		assertFile(item, "Entries", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(2);
		assertFile(item, "Entries.Log", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(3);
		assertFile(item, "Repository", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(4);
		assertFile(item, "Root", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(5);
		assertDir(item, "read", baseDir + "test_data");
		
		item = items.get(6);
		assertFile(item, ".hidden", baseDir + "test_data" + File.separator + "read");
		item = items.get(7);
		assertDir(item, "CVS", baseDir + "test_data" + File.separator + "read");
		item = items.get(8);
		assertFile(item, "Entries", baseDir + "test_data" + File.separator + "read" + File.separator + "CVS");
		item = items.get(9);
		assertFile(item, "Repository", baseDir + "test_data" +File.separator + "read" + File.separator + "CVS");
		item = items.get(10);
		assertFile(item, "Root", baseDir + "test_data" + File.separator + "read" + File.separator + "CVS");
		item = items.get(11);
		assertFile(item, "hello.txt", baseDir + "test_data" + File.separator + "read" );
		
		item = items.get(12);
		assertDir(item, "write", baseDir + "test_data");
		
		item = items.get(13);
		assertFile(item, ".cvsignore", baseDir + "test_data" + File.separator + "write");
		item = items.get(14);
		assertDir(item, "CVS", baseDir + "test_data" + File.separator + "write");
		item = items.get(15);
		assertFile(item, "Entries", baseDir + "test_data" +File.separator + "write" + File.separator + "CVS");
		item = items.get(16);
		assertFile(item, "Repository", baseDir + "test_data" + File.separator + "write" + File.separator + "CVS");
		item = items.get(17);
		assertFile(item, "Root", baseDir + "test_data" + File.separator + "write" + File.separator + "CVS");
	}

	public void assertFile(DiskItem item, String name, String pathPart) {
		assertEquals(name, item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains(pathPart));
	}
	
	public void assertDir(DiskItem item, String name, String pathPart) {
		assertEquals(name, item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains(pathPart));
	}
	
	public void testListContentsRecurseOne() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", 1);
		assertEquals(12,items.size());
		DiskItem item = items.get(0);
		assertDir(item, "CVS", baseDir + "test_data");
		item = items.get(1);
		assertFile(item, "Entries", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(2);
		assertFile(item, "Entries.Log", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(3);
		assertFile(item, "Repository", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(4);
		assertFile(item, "Root", baseDir + "test_data" + File.separator + "CVS");
		item = items.get(5);
		assertDir(item, "read", baseDir + "test_data");
		
		item = items.get(6);
		assertFile(item, ".hidden", baseDir + "test_data" + File.separator + "read");
		item = items.get(7);
		assertDir(item, "CVS", baseDir + "test_data" + File.separator + "read");
		item = items.get(8);
		assertFile(item, "hello.txt", baseDir + "test_data" + File.separator + "read" );
		
		item = items.get(9);
		assertDir(item, "write", baseDir + "test_data");
		
		item = items.get(10);
		assertFile(item, ".cvsignore", baseDir + "test_data" + File.separator + "write");
		item = items.get(11);
		assertDir(item, "CVS", baseDir + "test_data" + File.separator + "write");
	}

	
	
	public void testListContentsWithFilter() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", txtFileFilter, 0);
		assertEquals(0, items.size());
	}
	
	
	public void testListContentsWithFilterInfinateRecurse() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", txtFileFilter,  -1);
		assertEquals(1, items.size());
		DiskItem item = items.get(0);
		assertEquals("hello.txt", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains(baseDir + "test_data" + File.separator + "read"));
		
	}
	
	public void testListContentsWithFilterRecurseOne() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents(baseDir + "test_data", txtFileFilter,  1);
		assertEquals(1, items.size());
		DiskItem item = items.get(0);
		assertEquals("hello.txt", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains(baseDir + "test_data" + File.separator + "read"));
	}
}
