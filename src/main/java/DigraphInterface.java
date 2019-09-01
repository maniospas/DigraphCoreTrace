import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import analysis.Importer;
import trace.CacheDigraph;
import trace.SimpleDigraph;
import trace.SparceMatrix;
import trace.Trace;
import util.ThreadManager;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.awt.event.ActionEvent;
import javax.swing.JProgressBar;
import javax.swing.ToolTipManager;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.SwingConstants;
import javax.swing.JCheckBoxMenuItem;


public class DigraphInterface extends JFrame {
	private static final long serialVersionUID = 1197219200482379833L;
	
	public static class MethodEntry implements Serializable {
		private static final long serialVersionUID = -9058969862890446684L;
		private String name;
		private String details;
		private int index;
		private double score;
		private SimpleDigraph trace;
		private int numEdges = -1;
		private int outgoing = 0;
		public MethodEntry(String name, String details, int index) {
			this.name = name;
			this.index = index;
			this.details = details;
			score = 0;
			trace = null;
		}
		public void setTrace(SimpleDigraph trace, double score, int outgoing) {
			this.trace = trace;
			this.score = score;
			this.outgoing = outgoing;
			numEdges = -1;
		}
		@Override
		public String toString() {
			return name;
		}
		public String getName() {
			return name;
		}
		public String getDetails() {
			return details;
		}
		public double getScore() {
			return score;
		}
		public SimpleDigraph getTrace() {
			return trace;
		}
		public int getIndex() {
			return index;
		}
		public double getParameter() {
			//score = getOutgoing()-a*getNumEdges()
			if(getNumEdges()==0)
				return 0;
			return (getOutgoing()-score)/getNumEdges();
		}
		public int getOutgoing() {
			return outgoing;
		}
		public int getNumEdges() {
			if(numEdges==-1) {
				if(trace==null)
					return 0;
				numEdges = trace.getNumEdges();
			}
			return numEdges;
		}
		public String dedscribeProperties() {
			return "ELOD: "+(int)getScore()+" for parameter "+(int)getParameter()+"   ("+getOutgoing()+" external edges, "+getNumEdges()+" edges)";
		}
	}
	

	private JPanel contentPane, displayPanel;
	
	//loaded project and properties
	private JPanel previousVisualizationPanel = null;
	private JPanel panel;
	private JPanel importPanel;
	
	private DefaultListModel<MethodEntry> methodListModel = new DefaultListModel<MethodEntry>();
	private JList<MethodEntry> methodList;
	private HashMap<Integer, MethodEntry> entries = new HashMap<Integer, MethodEntry>();
	private SimpleDigraph importingGraph;
	
	private Thread runningThread = null;
	private boolean runningThreadRunning = false;
	private JButton stopImport;
	private JProgressBar progressBar;
	private JMenuBar menuBar;
	private JMenu mnProject;
	private JMenuItem mntmExit;
	private JMenu mnTrace;
	private JMenuItem mntmExportTraceImage;
	private JMenuItem mntmShowWholeProject;
	private JMenu mnLayout;
	private JRadioButtonMenuItem rdbtnmntmTree;
	private JRadioButtonMenuItem rdbtnmntmIsomap;
	private JMenu mnTraceParameter;
	private JSlider slider;
	private MethodEntry lastSelectedEntry = null;
	private JMenuItem mntmSave;
	private JMenu mnLoad;
	private JMenuItem mntmLoad;
	private JMenuItem mntmSources;
	private JMenuItem mntmBin;
	private JMenuItem mntmAutodetect;
	private JMenu mnCores;
	private JMenu mnFilter;
	private JTextField filterName;
	private JPanel panel_1;
	private JLabel lblMinElod;
	private JSlider sliderScore;
	private JPanel panel_2;
	private JLabel lblMinSize;
	private JSlider sliderEdges;
	private JMenuItem mntmApplyFilters;
	private JMenu mnTooltip;
	private JMenuItem mntmApply;
	private JPanel panel_3;
	private JLabel lblMinParameter;
	private JSlider sliderParameter;
	private JCheckBoxMenuItem chckbxmntmCalledMethods;
	private JCheckBoxMenuItem chckbxmntmSourceCode;
	private JCheckBoxMenuItem chckbxmntmTraceInformation;
	
	
	public boolean passesFilters(MethodEntry entry) {
		return entry.getTrace()!=null && entry.getScore()>=sliderScore.getValue() && entry.getNumEdges()>=sliderEdges.getValue() && entry.getParameter()>=sliderParameter.getValue() && entry.getName().contains(filterName.getText());
	}
	
	public void updateFilters() {
		if(importingGraph==null)
			return;
		
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				if(runningThread!=null) {
					if(runningThreadRunning) {
						try {
							runningThread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					runningThreadRunning = false;
				}
				if(previousVisualizationPanel!=null)
					displayPanel.remove(previousVisualizationPanel);
				if(!runningThreadRunning) {
					if(methodList.getSelectedValue()!=null)
						lastSelectedEntry = methodList.getSelectedValue();
					runningThreadRunning = true;
					progressBar.setValue(0);
					runningThread = new Thread() {
						public void run() {
							methodListModel.removeAllElements();
							progressBar.setVisible(true);
							stopImport.setVisible(true);
							progressBar.setIndeterminate(false);
							progressBar.setStringPainted(true);
							revalidate();
							repaint();
							progressBar.setMaximum(entries.size());
							final int[] numberOfCompletedThreads = new int[1];
							for(int i=0;i<importingGraph.getNumNodes() && runningThreadRunning;i++) {
								final int id = i;
								ThreadManager.scheduleRunnable(new Runnable() {
									public void run() {
										if(passesFilters(entries.get(id)))
											EventQueue.invokeLater(new Runnable(){
												public void run() {
													methodListModel.addElement(entries.get(id));
												}
											});
										numberOfCompletedThreads[0]++;
										progressBar.setValue(numberOfCompletedThreads[0]);
									}
								});
								ThreadManager.waitForNextScheduleOpening();//it's more correct in the begining of the schedule task, but here improves responsiveness for halting
							}
							ThreadManager.synchronizeAll();
							progressBar.setVisible(false);
							stopImport.setVisible(false);
							runningThreadRunning = false;
							if(lastSelectedEntry!=null && passesFilters(lastSelectedEntry))
								methodList.setSelectedValue(lastSelectedEntry, true);
							else
								lastSelectedEntry = null;
							revalidate();
							repaint();
							extractTraceForSelection();
						}
					};
					runningThread.start();
				}
			}
		});
	}
	
	private void updateTracesOnly(final int max_a) {
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				if(runningThread!=null) {
					if(runningThreadRunning) {
						runningThreadRunning = false;
						try{
							runningThread.interrupt();
						}
						catch(Exception e) {
						}
						try {
							runningThread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				if(previousVisualizationPanel!=null)
					displayPanel.remove(previousVisualizationPanel);
				if(!runningThreadRunning) {
					if(methodList.getSelectedValue()!=null)
						lastSelectedEntry = methodList.getSelectedValue();
					runningThreadRunning = true;
					progressBar.setValue(0);
					runningThread = new Thread() {
						public void run() {
							methodListModel.removeAllElements();
							progressBar.setVisible(true);
							stopImport.setVisible(true);
							progressBar.setIndeterminate(false);
							progressBar.setStringPainted(true);
							revalidate();
							repaint();
							progressBar.setMaximum(entries.size());
							final int[] numberOfCompletedThreads = new int[1];
							for(int i=0;i<importingGraph.getNumNodes() && runningThreadRunning;i++) {
								final int id = i;
								ThreadManager.scheduleRunnable(new Runnable() {
									public void run() {
										SimpleDigraph trace = Trace.coreTrace(importingGraph, id, null, max_a);
										double score = trace!=null && trace.metaScore!=null?trace.metaScore[id]:0;
										int outgoing = (int)Math.round(importingGraph.linksWithin(trace, 1, 0));
										entries.get(id).setTrace(trace, score, outgoing);
										if(passesFilters(entries.get(id)))
											EventQueue.invokeLater(new Runnable(){
												public void run() {
													methodListModel.addElement(entries.get(id));
												}
											});
										numberOfCompletedThreads[0]++;
										progressBar.setValue(numberOfCompletedThreads[0]);
									}
								});
								ThreadManager.waitForNextScheduleOpening();//it's more correct in the begining of the schedule task, but here improves responsiveness for halting
							}
							ThreadManager.synchronizeAll();
							progressBar.setVisible(false);
							stopImport.setVisible(false);
							runningThreadRunning = false;
							methodList.setSelectedValue(lastSelectedEntry, true);
							revalidate();
							repaint();
							extractTraceForSelection();
						}
					};
					runningThread.start();
				}
			}
		});
	}
	
	private void updateProject(final String path, final boolean importSources, final boolean importBinary) {
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				if(runningThread!=null) {
					if(runningThreadRunning)
						try {
							runningThread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					runningThreadRunning = false;
				}
				if(previousVisualizationPanel!=null)
					displayPanel.remove(previousVisualizationPanel);
				if(!runningThreadRunning) {
					methodListModel.removeAllElements();
					runningThreadRunning = true;
					progressBar.setValue(0);
					runningThread = new Thread() {
						public void run() {
							progressBar.setVisible(true);
							stopImport.setVisible(true);
							progressBar.setIndeterminate(true);
							progressBar.setStringPainted(false);
							revalidate();
							repaint();
							System.out.println("Path: "+path);
							
							Importer importer = new analysis.code.ASTProjectImporter();
							SparceMatrix callMatrix = null;
							try {
								importer.load(new File(path));
								callMatrix = importer.createCallGraph();
								System.out.println("Methods: "+callMatrix.size());
								System.out.println("Calls  : "+callMatrix.countNonZeros());
							} catch (Exception e) {
								e.printStackTrace();
							}
							if(callMatrix!=null && callMatrix.size()!=0) {
								importingGraph = new CacheDigraph(callMatrix);
								importingGraph.removeSelfLoops();
								for(int i=0;i<importingGraph.getNumNodes() && runningThreadRunning;i++) {
									if(importer.getMethod(i)==null) {
										System.out.println(i+"/"+importingGraph.getNumNodes());
										continue;
									}
									String methodName = importer.getMethod(i).toString();
									String methodDetails = "";
									entries.put(i, new MethodEntry(methodName, methodDetails, i));
								}
								
								progressBar.setIndeterminate(false);
								progressBar.setStringPainted(true);
								progressBar.setMaximum(importingGraph.getNumNodes());
								final int[] numberOfCompletedThreads = new int[1];
								for(int i=0;i<importingGraph.getNumNodes() && runningThreadRunning;i++) {
									final int id = i;
									ThreadManager.scheduleRunnable(new Runnable() {
										public void run() {
											SimpleDigraph trace = Trace.coreTrace(importingGraph, id, null, slider.getValue()==slider.getMaximum()?Integer.MAX_VALUE:slider.getValue());
											double score = trace!=null && trace.metaScore!=null?trace.metaScore[id]:0;
											int outgoing = (int)Math.round(importingGraph.linksWithin(trace, 1, 0));
											entries.get(id).setTrace(trace, score, outgoing);
											if(passesFilters(entries.get(id)) && score>=1)
												EventQueue.invokeLater(new Runnable(){
													public void run() {
														methodListModel.addElement(entries.get(id));
													}
												});
											numberOfCompletedThreads[0]++;
											progressBar.setValue(numberOfCompletedThreads[0]);
										}
									});
									ThreadManager.waitForNextScheduleOpening();//it's more correct in the begining of the schedule task, but here improves responsiveness for halting
								}
							}
							ThreadManager.synchronizeAll();
							progressBar.setVisible(false);
							stopImport.setVisible(false);
							runningThreadRunning = false;
							revalidate();
							repaint();
						}
					};
					runningThread.start();
				}
			}
		});
	}
	
	public void extractTraceForSelection() {
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				MethodEntry entry = methodList.getSelectedValue();
				if(previousVisualizationPanel!=null)
					displayPanel.remove(previousVisualizationPanel);
				if(entry!=null) {
					String[] names = new String[entries.size()];
					String[] tooltips = new String[entries.size()];
					for(MethodEntry e : entries.values())
						names[e.getIndex()] = e.getName();
					for(MethodEntry e : entries.values()) {
						String details = "";
						if(chckbxmntmTraceInformation.isSelected())
							details += "\n"+e.dedscribeProperties()+"\n\n";
						if(chckbxmntmCalledMethods.isSelected()) {
							for(int s : importingGraph.getSuccessors(e.getIndex()))
								details += "  >  "+names[s]+"\n";
							details += "\n";
						}
						if(chckbxmntmSourceCode.isSelected())
							details += e.getDetails();
						while(details.endsWith("\n"))
							details = details.substring(0, details.length()-1);
						tooltips[e.getIndex()] = details;
					}
					if(rdbtnmntmTree.isSelected())
						previousVisualizationPanel = entry.getTrace().getJungVisualizationServer(names, tooltips, displayPanel.getWidth(), displayPanel.getHeight(), entry.getIndex());
					else
						previousVisualizationPanel = entry.getTrace().getJungVisualizationServer(names, tooltips, displayPanel.getWidth(), displayPanel.getHeight(), -1);
					displayPanel.add(previousVisualizationPanel);
					displayPanel.revalidate();
					displayPanel.repaint();
				}
			}
		});
	}
	
	private String getUserPath() {
		JFileChooser chooser = new JFileChooser(); 
	    chooser.setCurrentDirectory(new java.io.File("."));
	    chooser.setDialogTitle("Import project folder");
	    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    chooser.setAcceptAllFileFilterUsed(false);
	    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
	    	return chooser.getSelectedFile().getPath();
	    }
	    return "";
	}
	
	public void saveProject(String filename) {
		ObjectOutputStream oos = null;
		FileOutputStream fout = null;
		try{
		    fout = new FileOutputStream(filename, true);
		    oos = new ObjectOutputStream(fout);
		    oos.writeObject(importingGraph);
		    oos.writeObject(entries);
		} catch (Exception ex) {
		    ex.printStackTrace();
		} finally {
		    if(oos != null){
		        try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    } 
		}
	}
	
	@SuppressWarnings("unchecked")
	public void loadProject(String filename) throws Exception {
		ObjectInputStream objectinputstream = null;
		try {
		    FileInputStream streamIn = new FileInputStream(filename);
		    objectinputstream = new ObjectInputStream(streamIn);
		    importingGraph = (SimpleDigraph) objectinputstream.readObject();
		    entries = (HashMap<Integer, MethodEntry>) objectinputstream.readObject();
		    updateFilters();
		} catch (Exception e) {
		    throw e;
		} finally {
		    if(objectinputstream != null){
		        try {
					objectinputstream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    } 
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DigraphInterface frame = new DigraphInterface();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public DigraphInterface() {
		ToolTipManager.sharedInstance().setInitialDelay(0);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 900, 600);
		this.setMaximumSize(new Dimension(900, 600));
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		mnProject = new JMenu("Project");
		menuBar.add(mnProject);
		
		mnLoad = new JMenu("Create from...");
		mnProject.add(mnLoad);
		
		mntmAutodetect = new JMenuItem("Auto-Detect");
		mntmAutodetect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = getUserPath();
				if(!path.isEmpty()) 
					updateProject(path, true, true);
			}
		});
		mnLoad.add(mntmAutodetect);
		
		mntmBin = new JMenuItem("Binaries (bin/)");
		mntmBin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = getUserPath();
				if(!path.isEmpty()) 
					updateProject(path, false, true);
			}
		});
		mnLoad.add(mntmBin);
		
		mntmSources = new JMenuItem("Sources (src/)");
		mntmSources.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String path = getUserPath();
				if(!path.isEmpty()) 
					updateProject(path, true, false);
			}
		});
		mnLoad.add(mntmSources);
		
		mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
			    chooser.setCurrentDirectory(new java.io.File("."));
			    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			    chooser.setAcceptAllFileFilterUsed(true);
			    chooser.setDialogTitle("Save Project");
			    int retVal = chooser.showSaveDialog(null);
                try {
    			    if(retVal==JFileChooser.APPROVE_OPTION){
    			        File f = chooser.getSelectedFile();
    			        String test = f.getAbsolutePath();
    			        if(!test.endsWith(".core"))
    			        	test += ".core";
    					saveProject(test);
    			     }
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
			}
		});
		mnProject.add(mntmSave);
		
		mntmLoad = new JMenuItem("Load");
		mntmLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
			    chooser.setCurrentDirectory(new java.io.File("."));
			    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			    chooser.setAcceptAllFileFilterUsed(true);
			    FileNameExtensionFilter filter = new FileNameExtensionFilter("Core project files (*.core)", "core");
			    chooser.setDialogTitle("Load Project");
			    chooser.setFileFilter(filter);
			    
			    int retVal = chooser.showOpenDialog(null);
                try {
    			    if(retVal==JFileChooser.APPROVE_OPTION){
    			        File f = chooser.getSelectedFile();
    			        String test = f.getAbsolutePath();
    					loadProject(test);
    			     }
                } catch (Exception exp) {
                	JOptionPane.showMessageDialog(panel, "Not a valid core project file (*.core)", "Error", JOptionPane.ERROR_MESSAGE);
                }
			}
		});
		mnProject.add(mntmLoad);
		
		mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnProject.add(mntmExit);
		
		mnTrace = new JMenu("View");
		menuBar.add(mnTrace);
		
		mntmShowWholeProject = new JMenuItem("Show Whole Project");
		mntmShowWholeProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(importingGraph!=null) {
					if(previousVisualizationPanel!=null)
						displayPanel.remove(previousVisualizationPanel);
					String[] names = new String[entries.size()];
					String[] tooltips = new String[entries.size()];
					for(MethodEntry entry : entries.values()) {
						names[entry.getIndex()] = entry.getName();
						tooltips[entry.getIndex()] = entry.getDetails();
					}
					previousVisualizationPanel = importingGraph.getJungVisualizationServer(names, tooltips, displayPanel.getWidth(), displayPanel.getHeight(), -1);
					displayPanel.add(previousVisualizationPanel);
					displayPanel.revalidate();
					displayPanel.repaint();
				}
			}
		});
		mnTrace.add(mntmShowWholeProject);
		
		mntmExportTraceImage = new JMenuItem("Export Trace Image");
		mntmExportTraceImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int w = displayPanel.getWidth();
			    int h = displayPanel.getHeight();
			    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			    Graphics2D g = bi.createGraphics();
			    displayPanel.paint(g);
			    JFileChooser chooser = new JFileChooser();
			    chooser.setCurrentDirectory(new java.io.File("."));
			    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			    chooser.setAcceptAllFileFilterUsed(true);
			    int retVal = chooser.showSaveDialog(null);
                try {
    			    if(retVal==JFileChooser.APPROVE_OPTION){
    			        File f = chooser.getSelectedFile();
    			        String test = f.getAbsolutePath();
    			        ImageIO.write(bi,"png",new File(test));
    			     }
                } catch (IOException exp) {
                    exp.printStackTrace();
                }
			}
		});
		mnTrace.add(mntmExportTraceImage);
		
		mnLayout = new JMenu("Layout");
		mnTrace.add(mnLayout);
		
		rdbtnmntmTree = new JRadioButtonMenuItem("Tree");
		rdbtnmntmTree.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				rdbtnmntmTree.setSelected(true);
				rdbtnmntmIsomap.setSelected(false);
				extractTraceForSelection();
			}
		});
		rdbtnmntmTree.setSelected(true);
		mnLayout.add(rdbtnmntmTree);
		
		rdbtnmntmIsomap = new JRadioButtonMenuItem("ISOMap");
		rdbtnmntmIsomap.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rdbtnmntmTree.setSelected(false);
				rdbtnmntmIsomap.setSelected(true);
				extractTraceForSelection();
			}
		});
		mnLayout.add(rdbtnmntmIsomap);
		
		mnTooltip = new JMenu("Tooltip Text");
		mnTrace.add(mnTooltip);
		
		chckbxmntmCalledMethods = new JCheckBoxMenuItem("Called Methods");
		chckbxmntmCalledMethods.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extractTraceForSelection();
			}
		});
		
		chckbxmntmTraceInformation = new JCheckBoxMenuItem("Trace Information");
		chckbxmntmTraceInformation.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extractTraceForSelection();
			}
		});
		mnTooltip.add(chckbxmntmTraceInformation);
		chckbxmntmCalledMethods.setSelected(true);
		mnTooltip.add(chckbxmntmCalledMethods);
		
		chckbxmntmSourceCode = new JCheckBoxMenuItem("Source Code");
		chckbxmntmSourceCode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				extractTraceForSelection();
			}
		});
		mnTooltip.add(chckbxmntmSourceCode);
		
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( 2 ), new JLabel("2") );
		labelTable.put( new Integer( 4 ), new JLabel("4") );
		labelTable.put( new Integer( 6 ), new JLabel("6") );
		labelTable.put( new Integer( 8 ), new JLabel("8") );
		labelTable.put( new Integer( 9 ), new JLabel("Inf") );
		
		mnCores = new JMenu("Trace");
		menuBar.add(mnCores);
		
		mnTraceParameter = new JMenu("Max ELOD Parameter");
		mnCores.add(mnTraceParameter);
		
		slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				//if(importingGraph!=null)
					//updateTracesOnly(slider.getValue()==slider.getMaximum()?Integer.MAX_VALUE:slider.getValue());
			}
		});
		slider.setMinimum(2);
		slider.setPaintLabels(true);
		slider.setMaximum(9);
		slider.setValue(10);
		slider.setLabelTable( labelTable );
		
		mnTraceParameter.add(slider);
		
		mntmApply = new JMenuItem("Recalculate Project Traces");
		mntmApply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(importingGraph!=null)
					updateTracesOnly(slider.getValue()==slider.getMaximum()?Integer.MAX_VALUE:slider.getValue());
			}
		});
		mnTraceParameter.add(mntmApply);
		
		mnFilter = new JMenu("Filter");
		mnCores.add(mnFilter);
		
		panel_1 = new JPanel();
		mnFilter.add(panel_1);
		
		lblMinElod = new JLabel("Min ELOD");
		lblMinElod.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinElod.setPreferredSize(new Dimension(90,20));
		panel_1.add(lblMinElod);
		
		labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put( new Integer( 0 ), new JLabel("0") );
		labelTable.put( new Integer( 2 ), new JLabel("2") );
		labelTable.put( new Integer( 4 ), new JLabel("4") );
		labelTable.put( new Integer( 6 ), new JLabel("6") );
		labelTable.put( new Integer( 8 ), new JLabel("8") );
		labelTable.put( new Integer( 10 ), new JLabel("10") );
		
		sliderScore = new JSlider();
		sliderScore.setLabelTable(labelTable);
		sliderScore.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				//updateFilters();
			}
		});
		sliderScore.setPaintLabels(true);
		sliderScore.setValue(1);
		sliderScore.setMaximum(10);
		panel_1.add(sliderScore);
		
		panel_3 = new JPanel();
		mnFilter.add(panel_3);
		
		lblMinParameter = new JLabel("Min Parameter");
		lblMinParameter.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinParameter.setPreferredSize(new Dimension(90,20));
		panel_3.add(lblMinParameter);
		
		sliderParameter = new JSlider();
		sliderParameter.setValue(0);
		sliderParameter.setLabelTable(labelTable);
		sliderParameter.setPaintLabels(true);
		sliderParameter.setMaximum(10);
		panel_3.add(sliderParameter);
		
		panel_2 = new JPanel();
		mnFilter.add(panel_2);
		
		lblMinSize = new JLabel("Min Edges");
		lblMinSize.setHorizontalAlignment(SwingConstants.LEFT);
		lblMinSize.setPreferredSize(new Dimension(90, 20));
		panel_2.add(lblMinSize);
		
		sliderEdges = new JSlider();
		sliderEdges.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				//updateFilters();
			}
		});
		sliderEdges.setLabelTable(labelTable);
		sliderEdges.setPaintLabels(true);
		sliderEdges.setMaximum(10);
		sliderEdges.setValue(0);
		panel_2.add(sliderEdges);

		filterName = new JTextField();
		mnFilter.add(filterName);
		
		
		mntmApplyFilters = new JMenuItem("Apply Filter");
		mntmApplyFilters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});
		
		mnFilter.add(mntmApplyFilters);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		displayPanel = new JPanel();
		contentPane.add(displayPanel, BorderLayout.CENTER);
		displayPanel.setDoubleBuffered(true);
		
		panel = new JPanel();
		contentPane.add(panel, BorderLayout.WEST);
		panel.setLayout(new BorderLayout(0, 0));
		
		methodList = new JList<MethodEntry>();
		methodList.setModel(methodListModel);
		methodList.setCellRenderer(new MethodRenderer());
		methodList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent arg0) {
				extractTraceForSelection();
			}
		});
		JScrollPane scrollPane = new JScrollPane(methodList);
		scrollPane.setPreferredSize(new Dimension(400, -1));
		panel.add(scrollPane, BorderLayout.CENTER);
		
		importPanel = new JPanel();
		importPanel.setPreferredSize(new Dimension(400, 15));
		panel.add(importPanel, BorderLayout.NORTH);
		importPanel.setLayout(new BorderLayout(0, 0));
		
		stopImport = new JButton("Stop");
		stopImport.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				runningThreadRunning = false;
			}
		});
		stopImport.setVisible(false);
		scrollPane.setPreferredSize(new Dimension(100, -1));
		importPanel.add(stopImport, BorderLayout.EAST);
		
		progressBar = new JProgressBar();
		scrollPane.setPreferredSize(new Dimension(300, -1));
		progressBar.setStringPainted(true);
		importPanel.add(progressBar, BorderLayout.CENTER);
		progressBar.setVisible(false);
	}
	
	
	public class MethodRenderer extends JLabel implements ListCellRenderer<MethodEntry> {
		private static final long serialVersionUID = -6574024643343878247L;
		public MethodRenderer() {
	        setOpaque(true);
	    }
	    public Component getListCellRendererComponent(JList<? extends MethodEntry> list, MethodEntry method, int index, boolean isSelected, boolean cellHasFocus) {
	        setText("<html><font size=\"4\">"+method.getName()+"<br/></font></big>&nbsp &nbsp&nbsp "+method.dedscribeProperties()+"</html>");
	        this.setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
	    	if (isSelected) {
	    	    setBackground(list.getSelectionBackground());
	    	    setForeground(list.getSelectionForeground());
	    	} else {
	    	    setBackground(list.getBackground());
	    	    setForeground(list.getForeground());
	    	}
	        return this;
	    }
	}
	
}
