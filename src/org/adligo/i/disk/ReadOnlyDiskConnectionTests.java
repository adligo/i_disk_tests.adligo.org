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
		assertFalse("running in " + new File(".").getAbsolutePath(), con.checkIfFileExists("test_data/foo.txt"));
		assertTrue("running in " + new File(".").getAbsolutePath(),con.checkIfFileExists("test_data/read/hello.txt"));
		con.returnToPool();
	}
	
	public void testCheckIfDirectoryExists() {
		ReadOnlyDiskConnection con = pool.getConnection();
		assertFalse(con.checkIfDirectoryExists("test_data/foo"));
		assertTrue(con.checkIfDirectoryExists("test_data/read"));
		con.returnToPool();
	}
	
	public void testCheckHidden() {
		ReadOnlyDiskConnection con = pool.getConnection();
		assertFalse(con.checkIfHidden("test_data/foo"));
		assertTrue(con.checkIfHidden("test_data/read/.hidden"));
		con.returnToPool();
	}
	
	
	public void testCheckLastModified() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getModifiedTime("test_data/read/hello.txt");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		Long mod2 = con.getModifiedTime("test_data/read/hello.txt");
		assertEquals(mod, mod2);
		
		mod2 = con.getModifiedTime("notAFile.txt");
		assertNull(mod2);
		con.returnToPool();
	}
	
	public void testGetFreeSpace() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getFreeSpace("test_data/read");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		
		mod = con.getFreeSpace("dirNotThere");
		assertNull(mod);
		con.returnToPool();
	}
	
	public void testUsableSpace() {
		ReadOnlyDiskConnection con = pool.getConnection();
		Long mod = con.getUsableSpace("test_data/read");
		assertNotNull(mod);
		assertTrue(mod >= 1);
		
		mod = con.getUsableSpace("dirNotThere");
		assertNull(mod);
		con.returnToPool();
	}
	
	
	
	public void testReadFile() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		final StringBuffer sb = new StringBuffer();
		con.readFile("test_data/read/hello.txt", new I_InputProcessor() {
			
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
		
		
		con.readFile("test_data/read/hello.txt", new I_InputProcessor() {
			
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
		
		List<DiskItem> items = con.listContents("test_data", 0);
		assertEquals(2, items.size());
		DiskItem item = items.get(0);
		assertEquals("read", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains("test_data"));
		
		item = items.get(1);
		assertEquals("write", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data"));
	}
	
	public void testListContentsInfinateRecurse() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents("test_data", -1);
		assretCleanTestData(items);
	}
	
	public void testListContentsRecurseOne() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents("test_data", 1);
		assretCleanTestData(items);
	}

	public void assretCleanTestData(List<DiskItem> items) {
		assertEquals(4, items.size());
		DiskItem item = items.get(0);
		assertEquals("read", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains("test_data"));
		
		item = items.get(1);
		assertEquals(".hidden", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data" + File.separator + "read"));
		
		item = items.get(2);
		assertEquals("hello.txt", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data" + File.separator + "read"));
		
		item = items.get(3);
		assertEquals("write", item.getName());
		assertTrue(item.isDirectory());
		assertFalse(item.isFile());
		path = item.getPath();
		assertTrue(path.contains("test_data"));
	}
	
	
	public void testListContentsWithFilter() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents("test_data", txtFileFilter, 0);
		assertEquals(0, items.size());
	}
	
	
	public void testListContentsWithFilterInfinateRecurse() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents("test_data", txtFileFilter,  -1);
		assertEquals(1, items.size());
		DiskItem item = items.get(0);
		assertEquals("hello.txt", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains("test_data" + File.separator + "read"));
		
	}
	
	public void testListContentsWithFilterRecurseOne() throws Exception {
		ReadOnlyDiskConnection con = pool.getConnection();
		
		List<DiskItem> items = con.listContents("test_data", txtFileFilter,  1);
		assertEquals(1, items.size());
		DiskItem item = items.get(0);
		assertEquals("hello.txt", item.getName());
		assertFalse(item.isDirectory());
		assertTrue(item.isFile());
		String path = item.getPath();
		assertTrue(path.contains("test_data" + File.separator + "read"));
	}
}
