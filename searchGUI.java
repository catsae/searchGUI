import java.awt.EventQueue;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.JEditorPane;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
//import org.eclipse.wb.swing.FocusTraversalOnArray;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

class MyTask extends TimerTask{
	private searchGUI main;
	private String jobName;
	public MyTask(searchGUI this_instance, String jobName) {
		this.main = this_instance;
		this.jobName = jobName;
	}

	public void run() {
		JSONObject obj =  new JSONObject(searchGUI.HttpMethod("GET", "https://dataproc.googleapis.com/v1beta2/projects/" + main.projectId +"/regions/" + main.clusterRegion +"/jobs/" + main.jobId + "?key=" + main.apiKey, null, null, searchGUI.accessToken));
		if(obj.has("done")) {
			main.logPosition = obj.getString("driverOutputResourceUri");
			System.out.println(obj.getJSONObject("status").getString("state"));
			boolean success = obj.getJSONObject("status").getString("state").equals("DONE") ? true : false;
			main.endTime = obj.getJSONObject("status").getString("stateStartTime");
			JSONArray arr = obj.getJSONArray("statusHistory");
			for(int i=0;i<arr.length();i++) {
				if(arr.getJSONObject(i).getString("state").equals("PENDING")) {
					main.startTime = arr.getJSONObject(i).getString("stateStartTime");
					break;
				}
			}
			if(jobName.equals("InvertedIndex"))
				main.IIafterJob(success);
			else if(jobName.equals("TopN"))
				main.TopNafterJob(success);
			this.cancel();
		}
	}
}

public class searchGUI {
	public String projectId = "cs1660-final-project";
	public String bucketName = "dataproc-staging-us-west1-781616316318-ady5tlpd";
	public String clusterRegion = "us-west1";
	public String clusterName = "cluster-f35e";
	public static String accessToken;
	public String apiKey = "AIzaSyCz2QoTEbTHRZK1F8Zq_w3cAzBYr_EHR1A";
	public String jobId; 
	public String logPosition;
	public String startTime;
	public String endTime;
	private String IItext;
	
	private JFrame frame;
	private JButton btnShowLog;
	private JLabel lblLoading;
	private JButton btnLoadEngine;
	private JLabel lblElapsedTime;
	private File files[];
	private JTextField searchTextField;
	private JTable tableSearch = null;
	private JButton btnSearchForTerm;
	private JButton btnTopN;
	private JTextField topNTextField;
	private JTable tableTopN;
	private JScrollPane scrollPane;
	private JLayeredPane layeredPane_3;
	private JLayeredPane layeredPane_4;
	private JLabel lblTopNFail;
	private JLabel labelTopNElapsedTime;
	private JButton btnShowII;
	private JButton btnGenerate;
	private int N;

	private searchGUI getThis() {
		return this;
	}
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		accessToken = System.getenv("ACCESS_TOKEN");
		System.out.println(accessToken);
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					searchGUI window = new searchGUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public static String HttpMethod(String type, String url, String contentType, HttpEntity entity, String accessToken) {
		try {
			HttpClient client = HttpClientBuilder.create().build();;
			HttpRequestBase request = type.equals("POST") ? new HttpPost(url) : (type.equals("DELETE")) ? new HttpDelete(url) : (type.equals("PATCH")) ? new HttpPatch(url) : new HttpGet(url);
			request.addHeader("Authorization", "Bearer " + accessToken);
			if(contentType != null)
				request.addHeader("Content-Type", contentType);
			
			if(type.equals("GET"))
				request.addHeader("Cache-Control","no-cache, max-age=0");
			
			if(entity != null) {
				if(type.equals("POST"))
					((HttpPost)request).setEntity(entity);
				else if(type.equals("PATCH"))
					((HttpPatch)request).setEntity(entity);
			}
				
		
			HttpResponse response = client.execute(request);
			if(type.equals("DELETE"))
				return null;
			InputStream in = response.getEntity().getContent();
			String body = IOUtils.toString(in, "UTF-8");
			System.out.println(body);
			return body;
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}				
	}
	
	public void IIafterJob(boolean success) {
		btnShowLog.setVisible(true);
		btnShowII.setVisible(true);
		lblLoading.setText(success ? "<html>Job Success.<br/> Constructed Inverted Index!<br/>you can check log.</html>" : "Job Failed");
		for (File f: files) {
			HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/input" + "%2F" + f.getName(), null, null, accessToken);
		}
		
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date startDate = formatter.parse(startTime.substring(0, 19));
			Date endDate = formatter.parse(endTime.substring(0, 19));
		    long diff = (endDate.getTime() - startDate.getTime()) / 1000;
		    lblElapsedTime.setText("Elapsed Time: " + Long.toString(diff));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(!success) return;
		
		IItext = HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "II.txt" +"?alt=media", null, null, accessToken);
		btnSearchForTerm.setVisible(true);
		btnTopN.setVisible(true);
	}
	
	public void TopNafterJob(boolean success) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date startDate = formatter.parse(startTime.substring(0, 19));
			Date endDate = formatter.parse(endTime.substring(0, 19));
		    long diff = (endDate.getTime() - startDate.getTime()) / 1000;
		    System.out.println("Elapsed Time: " + diff);
		    labelTopNElapsedTime.setText("Top N Elapsed Time: " + Long.toString(diff));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(!success)
			lblTopNFail.setVisible(true);
		else{
			tableTopN = new JTable();
			tableTopN.setRowHeight(60);
			tableTopN.setFont(new Font("Tahoma", Font.PLAIN, 18));
			System.out.println("N: " + N);
			DefaultTableModel model = new DefaultTableModel();
			model.setColumnIdentifiers(new Object[] {"Term", "Frequency"});
			model.addRow(new Object[] {"Term", "Frequency"});
			
			String jsonBody = "{\"cacheControl\": \"no-cache, max-age=0\"}";
			try {
				HttpMethod("PATCH", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "TopN.txt", "application/json", new StringEntity(jsonBody), accessToken);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			String body = HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + "TopN.txt" +"?alt=media", null, null, accessToken);
			Scanner scanner = new Scanner(body);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] arr = line.split("\t");
				if(arr.length == 2)
					model.addRow(new Object[] {arr[0], arr[1]});
			}
			scanner.close();
			tableTopN.setModel(model);
			scrollPane = new JScrollPane(tableTopN);
			scrollPane.setBounds(233, 96, 459, 240);
			layeredPane_4.add(scrollPane);
		}
		
		btnGenerate.setEnabled(true);
		layeredPane_3.setVisible(false);
		layeredPane_4.setVisible(true);
	}
	
	
	
	/**
	 * Create the application.
	 */
	public searchGUI() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 951, 612);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane);
		
		JLayeredPane layeredPane_1 = new JLayeredPane();
		layeredPane_1.setBounds(0, 37, 935, 536);
		layeredPane_1.setVisible(false);
		frame.getContentPane().add(layeredPane_1);
		
		JLayeredPane layeredPane_2 = new JLayeredPane();
		layeredPane_2.setVisible(false);
		layeredPane_2.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_2);
		
		layeredPane_3 = new JLayeredPane();
		layeredPane_3.setVisible(false);
		layeredPane_3.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_3);
		
		layeredPane_4 = new JLayeredPane();
		layeredPane_4.setVisible(false);
		layeredPane_4.setBounds(0, 37, 935, 536);
		frame.getContentPane().add(layeredPane_4);
		
		JLabel lblEnterN = new JLabel("Enter N:");
		lblEnterN.setBounds(233, 66, 459, 85);
		layeredPane_3.add(lblEnterN);
		lblEnterN.setFont(new Font("Tahoma", Font.PLAIN, 40));
		lblEnterN.setHorizontalAlignment(SwingConstants.CENTER);
		
		topNTextField = new JTextField();
		topNTextField.setBounds(149, 158, 619, 48);
		layeredPane_3.add(topNTextField);
		topNTextField.setFont(new Font("Tahoma", Font.PLAIN, 18));
		topNTextField.setColumns(10);
		
		JButton buttonTopNGoBack = new JButton("Go Back");
		buttonTopNGoBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layeredPane_3.setVisible(false);
				layeredPane.setVisible(true);
			}
		});
		buttonTopNGoBack.setFont(new Font("Tahoma", Font.BOLD, 20));
		buttonTopNGoBack.setBounds(814, 0, 121, 48);
		layeredPane_3.add(buttonTopNGoBack);
		
		btnGenerate = new JButton("Generate!");
		btnGenerate.setBounds(233, 232, 459, 115);
		layeredPane_3.add(btnGenerate);
		btnGenerate.setFont(new Font("Tahoma", Font.PLAIN, 40));
		
		JButton btnTopNShowLog = new JButton("Show Log");
		btnTopNShowLog.setBounds(702, 67, 233, 85);
		layeredPane_4.add(btnTopNShowLog);
		btnTopNShowLog.setFont(new Font("Tahoma", Font.PLAIN, 40));
		
		JLabel lblN = new JLabel("N");
		lblN.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblN.setBounds(58, 51, 680, 48);
		layeredPane_4.add(lblN);
		
		lblTopNFail = new JLabel("Top N failed.");
		lblTopNFail.setVisible(false);
		lblTopNFail.setHorizontalAlignment(SwingConstants.CENTER);
		lblTopNFail.setFont(new Font("Tahoma", Font.BOLD, 40));
		lblTopNFail.setBounds(233, 162, 459, 120);
		layeredPane_4.add(lblTopNFail);
		
		labelTopNElapsedTime = new JLabel("Top N Elapsed Time");
		labelTopNElapsedTime.setVisible(false);
		labelTopNElapsedTime.setFont(new Font("Tahoma", Font.PLAIN, 18));
		labelTopNElapsedTime.setBounds(58, 51, 680, 48);
		layeredPane_4.add(labelTopNElapsedTime);
		
		JButton buttonGoBackToTopN = new JButton("Go Back To Top N");
		buttonGoBackToTopN.setFont(new Font("Tahoma", Font.PLAIN, 18));
		buttonGoBackToTopN.setBounds(738, 0, 197, 48);
		layeredPane_4.add(buttonGoBackToTopN);
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setMultiSelectionEnabled(true);
			
		JLabel lblFileToBe = new JLabel("File to Be Chosen");
		lblFileToBe.setBounds(0, 169, 935, 217);
		layeredPane.add(lblFileToBe);
		lblFileToBe.setVerticalAlignment(SwingConstants.TOP);
		lblFileToBe.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblFileToBe.setHorizontalAlignment(SwingConstants.CENTER);
		
		JButton btnChooseFile = new JButton("Choose File!");
		btnChooseFile.setFont(new Font("Tahoma", Font.PLAIN, 40));
		
		btnLoadEngine = new JButton("LoadEngine");

		btnLoadEngine.setVisible(false);
		btnLoadEngine.setBounds(233, 393, 459, 85);
		layeredPane.add(btnLoadEngine);
		btnLoadEngine.setFont(new Font("Tahoma", Font.PLAIN, 40));
		
		btnChooseFile.setBounds(233, 66, 459, 85);
		layeredPane.add(btnChooseFile);
		
		lblLoading = new JLabel("Loading...");
		lblLoading.setBounds(10, 162, 937, 210);
		lblLoading.setVisible(false);
		layeredPane.add(lblLoading);
		lblLoading.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblLoading.setHorizontalAlignment(SwingConstants.CENTER);
		
		btnShowLog = new JButton("Show Log");
		btnShowLog.setBounds(0, 66, 459, 85);
		layeredPane.add(btnShowLog);
		btnShowLog.setFont(new Font("Tahoma", Font.BOLD, 40));
		
		lblElapsedTime = new JLabel("");
		lblElapsedTime.setFont(new Font("Tahoma", Font.PLAIN, 15));
		lblElapsedTime.setBounds(12, 0, 136, 48);
		layeredPane.add(lblElapsedTime);
		
		btnSearchForTerm = new JButton("SearchForTerm");
		btnSearchForTerm.setVisible(false);
		btnSearchForTerm.setFont(new Font("Tahoma", Font.PLAIN, 40));
		btnSearchForTerm.setBounds(0, 393, 459, 85);
		layeredPane.add(btnSearchForTerm);
		
		
		btnTopN = new JButton("Top N");
		btnTopN.setVisible(false);
		btnTopN.setFont(new Font("Tahoma", Font.PLAIN, 40));
		btnTopN.setBounds(476, 393, 459, 85);
		layeredPane.add(btnTopN);
		
		btnShowII = new JButton("Show Inverted Indexing");
		btnShowII.setVisible(false);
		btnShowII.setFont(new Font("Tahoma", Font.BOLD, 30));
		btnShowII.setBounds(469, 66, 466, 85);
		layeredPane.add(btnShowII);
			
		
		JButton btnSearchBack = new JButton("Go Back");
		btnSearchBack.setBounds(814, 0, 121, 48);
		layeredPane_1.add(btnSearchBack);
		btnSearchBack.setFont(new Font("Tahoma", Font.BOLD, 20));
		
		JLabel lblEnterSearchTerm = new JLabel("Enter Search Term:");
		lblEnterSearchTerm.setBounds(233, 66, 459, 85);
		layeredPane_1.add(lblEnterSearchTerm);
		lblEnterSearchTerm.setHorizontalAlignment(SwingConstants.CENTER);
		lblEnterSearchTerm.setFont(new Font("Tahoma", Font.BOLD, 40));
		
		JButton btnSearch = new JButton("Search!");	
		searchTextField = new JTextField();
		searchTextField.setBounds(149, 158, 619, 48);
		layeredPane_1.add(searchTextField);
		searchTextField.setFont(new Font("Tahoma", Font.PLAIN, 18));
		searchTextField.setColumns(10);
		btnSearch.setBounds(233, 232, 459, 115);
		layeredPane_1.add(btnSearch);
		btnSearch.setFont(new Font("Tahoma", Font.PLAIN, 40));
		

		
		JButton btnGoBackToSearch = new JButton("Go Back To Search");
		btnGoBackToSearch.setBounds(738, 0, 197, 48);
		layeredPane_2.add(btnGoBackToSearch);
		btnGoBackToSearch.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		JLabel lblSearchedTerm = new JLabel("Searched Term");
		lblSearchedTerm.setBounds(58, 0, 680, 48);
		layeredPane_2.add(lblSearchedTerm);
		lblSearchedTerm.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		JLabel lblSearchElapsedTime = new JLabel("Search Elapsed Time");
		lblSearchElapsedTime.setBounds(58, 51, 680, 48);
		layeredPane_2.add(lblSearchElapsedTime);
		lblSearchElapsedTime.setFont(new Font("Tahoma", Font.PLAIN, 18));
		
		
		JLabel lblTermNotExist = new JLabel("Term Does Not Exist!");
		lblTermNotExist.setVisible(false);
		lblTermNotExist.setBounds(233, 162, 459, 120);
		layeredPane_2.add(lblTermNotExist);
		lblTermNotExist.setFont(new Font("Tahoma", Font.BOLD, 40));
		lblTermNotExist.setHorizontalAlignment(SwingConstants.CENTER);
		btnShowLog.setVisible(false);
		
		tableSearch = new JTable();
		tableSearch.setBounds(233, 120, 459, 240);
		tableSearch.setRowHeight(60);
		tableSearch.setFont(new Font("Tahoma", Font.PLAIN, 18));	
		layeredPane_2.add(tableSearch);
		
		

		JLabel lblNewLabel = new JLabel("Stanley Wang Search Engine");
		lblNewLabel.setBounds(0, 0, 935, 42);
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 20));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		frame.getContentPane().add(lblNewLabel);
		
		//frame.getContentPane().setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{layeredPane, lblLoading, lblNewLabel, lblFileToBe, btnLoadEngine, btnChooseFile}));
		
		btnChooseFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int res = fileChooser.showSaveDialog(null);
				if (res == JFileChooser.APPROVE_OPTION) {
					String text = "";
					
					files = fileChooser.getSelectedFiles();
					text = "<html><div style='text-align: center;'>";
					for (int n = 0; n < files.length; n++)
						text += files[n] + "<br/>";
					text += "</div></html>";
					
					lblFileToBe.setText(text);
					btnLoadEngine.setVisible(true);
				}
					
			}
		});
		
		btnLoadEngine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lblFileToBe.setVisible(false);
				btnChooseFile.setVisible(false);
				lblLoading.setVisible(true);
				btnLoadEngine.setVisible(false);
				
				for (File f: files) {
					String obj = HttpMethod("POST", "https://storage.googleapis.com/upload/storage/v1/b/" + bucketName + "/o??uploadType=media&name=input/" + f.getName(), "application/octet-stream", new FileEntity(f), accessToken);
					if(obj == null) {
						lblLoading.setText("Failed to upload files to bucket");
						return;
					}
				}
				lblLoading.setText("<html>Files Uploaded to bucket.<br/>Now Constructing II.txt on Cluster</html>");	
				
				String jsonBody = "{\"projectId\": \"" + projectId + "\"," +"\"job\": {\"placement\": {\"clusterName\": \"" + clusterName + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + bucketName +"/JAR/invertedindex.jar\"],\"args\": [\"gs://" + bucketName + "/input\",\"gs://" + bucketName + "/IIOutput\"],\"mainClass\": \"SSInvertedIndex\"}}}";
				try {
					JSONObject obj = new JSONObject(HttpMethod("POST", "https://dataproc.googleapis.com/v1/projects/" + projectId +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + apiKey, "application/json", new StringEntity(jsonBody), accessToken));
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTask task = new MyTask(getThis(), "InvertedIndex");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
		btnShowLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String url = logPosition.substring(5 + bucketName.length() + 1);
					url = url.replace("/", "%2f");
					url = "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + url + "%2E000000000" + "?alt=media";
					String body = HttpMethod("GET", url, null, null, accessToken);
					
					JDialog dialog = new JDialog(frame, "Job Log");
					dialog.setSize(500, 800);
					dialog.setVisible(true);
					JEditorPane panel = new JEditorPane();
					panel.setEditable(false);
					panel.setSize(500, 800);
					panel.setContentType("text/plain");
					panel.setText(body);
					panel.setVisible(true);
					JScrollPane scrollPanel = new JScrollPane(panel);
					scrollPanel.setSize(500, 800);
					scrollPanel.setVisible(true);
					dialog.getContentPane().add(scrollPanel);
					
				}catch (Exception e) {
					e.printStackTrace();
				}				
			}
		});
		
		btnShowII.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = new JDialog(frame, "Inverted Indexing .txt");
				dialog.setSize(500, 800);
				dialog.setVisible(true);
				JEditorPane panel = new JEditorPane();
				panel.setEditable(false);
				panel.setSize(500, 800);
				panel.setContentType("text/plain");
				panel.setText(IItext);
				panel.setVisible(true);
				JScrollPane scrollPanel = new JScrollPane(panel);
				scrollPanel.setSize(500, 800);
				scrollPanel.setVisible(true);
				dialog.getContentPane().add(scrollPanel);
			}
		});
		
		btnSearch.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				btnSearch.setText("Searching...");
				
				long start = System.currentTimeMillis();
			      
				String term = searchTextField.getText();
				List<String> docList = new ArrayList<String>(files.length);
				List<String> frequencyList = new ArrayList<String>(files.length);
				
				Scanner scanner = new Scanner(IItext);
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String[] arr = line.split("\t");
					if(term.equals(arr[0])) {
						System.out.println("Found term");
						for(int n=0;n < files.length; n++) {
							if(!arr[0].equals(term)) 
								break;
							
							docList.add(arr[1]);
							frequencyList.add(arr[2]);
							
							if(!scanner.hasNextLine()) 
								break;
							
							line = scanner.nextLine();
							arr = line.split("\t");
						}
						break;
					}
				}
				scanner.close();
				
				long end = System.currentTimeMillis();
				
				lblSearchedTerm.setText("Searched Term: " + term);
				lblSearchElapsedTime.setText("Search Elapsed Time: " + Float.toString((end - start) / 1000F));
				
				if(docList.size() == 0) {
					tableSearch.setModel(new DefaultTableModel());
					lblTermNotExist.setVisible(true);
				}
				else {						
					DefaultTableModel model = new DefaultTableModel();
				    model.setColumnIdentifiers(new Object[] {"Doc Name", "Frequency"});
				    model.addRow(new Object[] {"Doc Name", "Frequency"});
				    for(int n=0;n<docList.size();n++) 
				    	model.addRow(new Object[] {docList.get(n), frequencyList.get(n)});
					tableSearch.setModel(model);
				}
				layeredPane_1.setVisible(false);
				layeredPane_2.setVisible(true);	
			}
		});
		
		btnGenerate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!topNTextField.getText().matches("-?\\d+")) {
					topNTextField.setText("Enter positive number!");
					return;
				}
				N = Integer.parseInt(topNTextField.getText());
				if(N <= 0) {
					topNTextField.setText("Enter positive number!");
					return;
				}
				lblN.setText("N: " + N);
				
				btnGenerate.setText("Generating...");
				btnGenerate.setEnabled(false);
				String jsonBody = "{\"projectId\": \"" + projectId + "\"," +"\"job\": {\"placement\": {\"clusterName\": \"" + clusterName + "\"},\"hadoopJob\": {\"jarFileUris\": [\"gs://" + bucketName +"/JAR/topn.jar\"],\"args\": [\"gs://" + bucketName + "/IIOutput\",\"gs://" + bucketName + "/TopNOutput\",\"" + N + "\"],\"mainClass\": \"TopN\"}}}";
				try {
					JSONObject obj = new JSONObject(HttpMethod("POST", "https://dataproc.googleapis.com/v1/projects/" + projectId +"/regions/" + clusterRegion +"/jobs:submit" + "?key=" + apiKey, "application/json", new StringEntity(jsonBody), accessToken));
					jobId = obj.getJSONObject("reference").getString("jobId");
				} catch (UnsupportedEncodingException exc) {
					exc.printStackTrace();
				}
				java.util.Timer timer = new java.util.Timer();
				MyTask task = new MyTask(getThis(), "TopN");
				timer.scheduleAtFixedRate(task, 0, 5000);
			}
		});
		
		btnGoBackToSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnSearch.setText("Search!");
				lblTermNotExist.setVisible(false);
				layeredPane_2.setVisible(false);
				layeredPane_1.setVisible(true);
			}
		});
		
		
		buttonGoBackToTopN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopN.txt", null, null, accessToken);
		    	JSONObject obj= new JSONObject(HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=TopNOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		JSONArray arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopNOutput" + "%2f", null, null, accessToken);
		    	}
				btnGenerate.setText("Generate!");
				layeredPane_4.remove(scrollPane);
				lblTopNFail.setVisible(false);
				layeredPane_4.setVisible(false);
				layeredPane_3.setVisible(true);
			}
		});
		
		btnTopN.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				layeredPane.setVisible(false);
				layeredPane_3.setVisible(true);
			}
		});
		
		btnSearchForTerm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				layeredPane.setVisible(false);
				layeredPane_1.setVisible(true);
			}
		});
		
		btnSearchBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layeredPane_1.setVisible(false);
				layeredPane.setVisible(true);
			}
		});

		
		btnTopNShowLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					String url = logPosition.substring(5 + bucketName.length() + 1);
					url = url.replace("/", "%2f");
					url = "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + url + "%2E000000000" + "?alt=media";
					String body = HttpMethod("GET", url, null, null, accessToken);
					
					JDialog dialog = new JDialog(frame, "Job Log");
					dialog.setSize(500, 800);
					dialog.setVisible(true);
					JEditorPane panel = new JEditorPane();
					panel.setEditable(false);
					panel.setSize(500, 800);
					panel.setContentType("text/plain");
					panel.setText(body);
					panel.setVisible(true);
					JScrollPane scrollPanel = new JScrollPane(panel);
					scrollPanel.setSize(500, 800);
					scrollPanel.setVisible(true);
					dialog.getContentPane().add(scrollPanel);
					
				}catch (Exception exc) {
					exc.printStackTrace();
				}				

			}
		});
		
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/II.txt", null, null, accessToken);
		    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopN.txt", null, null, accessToken);
		    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/SearchTerm.txt", null, null, accessToken);
		    	
		    	
		    	JSONObject obj= new JSONObject(HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=IIOutput", null, null, accessToken));
		    	JSONArray arr;
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("IIOutput/")) 
			    			HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/IIOutput" + "%2f", null, null, accessToken);
			    	
		    	}
		    	
		    	obj= new JSONObject(HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=TopNOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("TopNOutput/")) 
			    			HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/TopNOutput" + "%2f", null, null, accessToken);
		    	}
		    	
		    	
		    	obj= new JSONObject(HttpMethod("GET", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o" + "?prefix=SearchTermOutput", null, null, accessToken));
		    	if(obj.has("items")) {
		    		arr = obj.getJSONArray("items");
			    	for(int i=0;i<arr.length();i++) {
			    		String fName = arr.getJSONObject(i).getString("name");
			    		if(!fName.equals("SearchTermOutput/")) 
			    			HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/" + fName.replace("/", "%2F"), null, null, accessToken);
			    	}
			    	HttpMethod("DELETE", "https://storage.googleapis.com/storage/v1/b/" + bucketName + "/o/SearchTermOutput" + "%2f", null, null, accessToken);
		    	}
		    	
		    }
		});
		
	}
}
