package de.lme.heartnhealth4u;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

import de.lme.heartnhealth4u.PanTompkins.QRS;
import de.lme.heartnhealth4u.PanTompkins.QRS.QrsArrhythmia;
import de.lme.heartnhealth4u.PanTompkins.QRS.QrsClass;
import de.lme.heartnhealth4u.PanTompkins.QRS.SegmentationStatus;
import de.lme.heartnhealth4u.ShimmerSimService.ShimConnection;
import de.lme.plotview.Plot;
import de.lme.plotview.Plot.PlotMarkerDefault;
import de.lme.plotview.Plot.PlotMarkerDefault.DefaultMark;
import de.lme.plotview.Plot.PlotStyle;
import de.lme.plotview.PlotView;
import de.lme.plotview.PlotView.PlotViewGroup;
import de.lme.plotview.SamplingPlot;





public class HeartyActivity extends AppCompatActivity implements IShimmerSimServiceCallback {
	private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");  // 스마트폰 - 아두이노 간 데이터 전송 UUID
	private BluetoothDevice device;
	private BluetoothSocket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	boolean deviceConnected=false;
	Thread thread;
	byte buffer[];
	int bufferPosition;
	boolean stopThread;
	int pulse;
	private String helpingHandContactNo;

	private String mBufferWritePos;




	public static final String TAG = ".HeartyActivity";

	private static final int DIALOG_EXIT = 1;
	private static final int DIALOG_RESULT = 2;

	public static final int NUM_PLOT_SLOTS = 6;

	public static final int PLOT_LIVE = 0;
	public static final int PLOT_PANTS_OUT = 1;
	public static final int PLOT_QRS = 2;
	public static final int PLOT_BEATS = 3;
	public static final int PLOT_NA1 = 4;
	public static final int PLOT_NA2 = 5;

	private GridView m_gridFeatures = null;

	private PlotView m_plotSlots[] = new PlotView[NUM_PLOT_SLOTS];
	private static Plot m_plots[] = new Plot[NUM_PLOT_SLOTS];

	private PlotViewGroup m_plotGroup = new PlotViewGroup();

	private static SamplingPlot m_plotFeat1 = null;
	private static SamplingPlot m_plotFeat2 = null;
	private static SamplingPlot m_plotFeat3 = null;

	private static de.lme.heartnhealth4u.PanTompkins m_pants = null;

	private ToneGenerator m_toneGen = null;
	private boolean m_playSound = true;


	private IShimmerSimService mIShimSimService = null;
	private ServiceConnection mConnection = null;

	// A handler on the UI thread.
	private Handler mHandler = null;

	private static ShimConnection m_con = new ShimConnection();
	private boolean m_isConnected = false;
	private static int m_samplingRate = 360;

	public static SimResult m_result = null;


	public static class SimResult {
		public String simName;

		public int numTotalBeatsRef = 0;
		public int numTotalBeats = 0;

		public int numTP = 0;
		public int numTN = 0;
		public int numFN = 0;
		public int numFP = 0;

		public int numTPBeats = 0;
		public int numTNBeats = 0;
		public int numFNBeats = 0;
		public int numFPBeats = 0;

		public int numNormalRef = 0;
		public int numNormal = 0;

		public int numPvcRef = 0;
		public int numPvc = 0;

		public int numAPCRef = 0;
		public int numAPC = 0;

		public int numFusedRef = 0;
		public int numFused = 0;

		public int numAVRef = 0;
		public int numAV = 0;

		public int numEscapeRef = 0;
		public int numEscape = 0;

		public int numBBBRef = 0;
		public int numBBB = 0;

		public int numAberrantRef = 0;
		public int numAberrant = 0;

		public int numOthersRef = 0;
		public int numOthers = 0;


		public SimResult(String name) {
			simName = name;
		}


		public int lastLabelCount = 0;
		public char currentLabel = '$';
		public char nextLabel = 0;
		public char lastLabel = 0;
		public boolean openLabel = false;


		public void newLabel(char label) {
			++lastLabelCount;

			if (lastLabelCount == 0) {
				lastLabel = currentLabel;
				currentLabel = nextLabel;
			}

			// if this beat is labeled, count it.
			if (label != 0) {
				// count total number of reference beats
				++numTotalBeatsRef;


				// the next label becomes active after the processing delay
				lastLabelCount = -de.lme.heartnhealth4u.PanTompkins.TOTAL_DELAY + 2;


				// don't count anything else while learning
				if (de.lme.heartnhealth4u.PanTompkins.learning) {
					nextLabel = label;
					return;
				}


				switch (label) {
					// PVC beat
					case 'V':
					case 'F':
						++numPvcRef;
						break;

					// fused beat
					case 'A':
					case 'a':
						++numAPCRef;
					case 'f':
						++numFusedRef;
						break;

					// normal beat
					case 'N':
					case '/':
						++numNormalRef;
						if (openLabel) {
							++numFNBeats;
						}
						openLabel = true;
						break;

					// escaped beat
					case 'E':
					case 'e':
					case 'n':
					case 'j':
						++numEscapeRef;
						break;

					case 'x':
						// "x == Non-conducted P-wave (blocked APC)" is just an annotation and will be counted as escape
						// beat,
						// but not increase the total number of beats
						++numAV;
						--numTotalBeatsRef;
						break;

					// bundle branch block beats
					case 'L':
					case 'R':
					case 'B':
						++numBBBRef;
						break;

					case '"':
					case '~':
					case '@':
					case '[':
					case ']':
					case '!':
					case '(':
					case ')':
					case 'p':
					case 't':
					case 'u':
					case '|':
					case '+':
					case 'D':
					case 'T':
					case '\'':
					case '`':
					case '*':
					case '=':
						// annotation, decrease total counter
						--numTotalBeatsRef;
						// keep old label
						label = nextLabel;
						break;

					case 'Q':
					case 'S':
					case 'r':
						++numAberrantRef;

						break;

					default:
					case 'J':
					case '?':
						++numOthersRef;
						break;
				}

				nextLabel = label;
			}
		}


		public void newBeat(QRS beat) {
			// count total beats
			++numTotalBeats;

			Log.d("newBeat", "[" + currentLabel + " - " + beat.classification);

			// don't count anything else while learning
			if (de.lme.heartnhealth4u.PanTompkins.learning)
				return;

			openLabel = false;

			if (currentLabel != '$') {
				++numTPBeats;
			} else {
				++numFPBeats;
			}

			if (beat.arrhythmia == QrsArrhythmia.AV_BLOCK) {
				++numAV;
				if (currentLabel == 'x')
					++numTP;
			} else if (beat.arrhythmia == QrsArrhythmia.FUSION) {
				++numFused;
			}

			if (beat.classification == QrsClass.NORMAL) {
				// count as normal beat
				if (currentLabel != '$') {
					if (currentLabel == 'N' || currentLabel == '/') {
						++numTN;
					} else {
						if (beat.arrhythmia != QrsArrhythmia.NONE)
							++numTP;
							// ignore x-annotations, they are often not annotated at the actual missing APC
							// ignore S-annotations if not detected, most S annotations in MIT-BIH-S records that are
							// non-ectopic are undetectable in the channel we parse

							// fused beats are ignored at this stage as they are checked in reverse for the last beat
						else if (currentLabel != 'x' && currentLabel != 'S' && currentLabel != 'A' && currentLabel != 'a'
								&& currentLabel != 'f') {
							++numFN;
						}
					}
				}

				++numNormal;
			} else {
				if (beat.classification == QrsClass.BB_BLOCK)
					++numBBB;
				else if (beat.classification == QrsClass.PVC || beat.classification == QrsClass.PVC_ABERRANT)
					++numPvc;
				else if (beat.classification == QrsClass.ESCAPE)
					++numEscape;
				else if (beat.classification == QrsClass.ABERRANT)
					++numAberrant;
				else if (beat.classification == QrsClass.APC || beat.classification == QrsClass.APC_ABERRANT)
					++numAPC;
				else {
					++numOthers;
				}

				if (currentLabel != '$') {
					// check previous beat if filter delay shifted the labels for fused beats
					if ((currentLabel != 'N' && currentLabel != '/') || lastLabel != 'N')
						++numTP;
					else {
						++numFP;
					}
				}
			}
		}


		private boolean isFinished = false;


		void finish() {
			if (isFinished)
				return;

			isFinished = true;

			if (numTotalBeatsRef > numTotalBeats) {
				numFN += numTotalBeatsRef - numTotalBeats;
			}
		}


		public String format() {  // Test Record 와 판별할 mitbih.csv 파일을 Data Source에서 선택했을 때 아래 코드 실행, 판별 결과를 바로 보여줌
			StringBuilder str = new StringBuilder();

			finish();

			str.append("판별 결과\n[Heart & Health For You] / [Reference]\nTP: ").append(numTP).append("\nTN: ")
					.append(numTN).append("\nFP: ").append(numFP).append("\nFN: ").append(numFN)
					.append("\n\nTotal Beats: ").append(numTotalBeats).append(" / ").append(numTotalBeatsRef)
					.append("\nNormal: ").append(numNormal).append(" / ").append(numNormalRef).append("\nPVC: ")
					.append(numPvc).append(" / ").append(numPvcRef).append("\nBB Block: ").append(numBBB)
					.append(" / ").append(numBBBRef).append("\nFused: ").append(numFused).append(" / ")
					.append(numFusedRef).append("\nEscape: ").append(numEscape).append(" / ").append(numEscapeRef)
					.append("\nAberrant: ").append(numAberrant).append(" / ").append(numAberrantRef).append("\nAPC: ")
					.append(numAPC).append(" / ").append(numAPCRef).append("\nAV: ").append(numAV).append(" / ")
					.append(numAVRef).append("\nOthers: ").append(numOthers).append(" / ").append(numOthersRef);

			return str.toString();
		}


		public void save(Context con) {  // 블루투스 연결을 하여 ECG 값을 얻었을 때 해당하는 코드, 코드를 보면 외부 저장소에 만들어진 패키지명 폴더에 결과 값이 txt파일로 저장되는거 같음 -> 확실하지 않음
			File f = null;

			//Context CSV_NAME = con;
			//String fileName = (CSV_NAME + (new SimpleDateFormat("_yyyy.MM.dd_HH.mm")).format(Calendar.getInstance().getTime()) + ".csv");
				try {
					f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "db_" + simName + "_result.txt");
//					con.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
					finish();

					FileWriter fw = new FileWriter(f, false);

					final StringBuilder sb = new StringBuilder(2048);


					sb.append("판별 결과\n[Heart & Health For You] / [Reference]\nTP: ").append(numTP).append("\nTN: ")
							.append(numTN).append("\nFP: ").append(numFP).append("\nFN: ").append(numFN)
							.append("\n\nTPBeats: ").append(numTPBeats).append("\nTNBeats: ").append(numTNBeats)
							.append("\nFPBeats: ").append(numFPBeats).append("\nFNBeats: ").append(numFNBeats)
							.append("\n\nTotal Beats: ").append(numTotalBeats).append(" / ").append(numTotalBeatsRef)
							.append("\nNormal: ").append(numNormal).append(" / ").append(numNormalRef).append("\nPVC: ")
							.append(numPvc).append(" / ").append(numPvcRef).append("\nBB Block: ").append(numBBB)
							.append(" / ").append(numBBBRef).append("\nFused: ").append(numFused).append(" / ")
							.append(numFusedRef).append("\nEscape: ").append(numEscape).append(" / ").append(numEscapeRef)
							.append("\nAberrant: ").append(numAberrant).append(" / ").append(numAberrantRef)
							.append("\nAPC: ").append(numAPC).append(" / ").append(numAPCRef).append("\nAV: ")
							.append(numAV).append(" / ").append(numAVRef).append("\nOthers: ").append(numOthers)
							.append(" / ").append(numOthersRef).append("\n\n\n").append(numTP).append("\t").append(numTN)
							.append("\t").append(numFP).append("\t").append(numFN).append("\t\t").append(numTPBeats)
							.append("\t").append(numTNBeats).append("\t").append(numFPBeats).append("\t")
							.append(numFNBeats).append("\t\t").append(numTotalBeats).append("\t").append(numTotalBeatsRef)
							.append("\t\t").append(numNormal).append("\t").append(numNormalRef).append("\t\t")
							.append(numPvc).append("\t").append(numPvcRef).append("\t\t").append(numAPC).append("\t")
							.append(numAPCRef).append("\t\t").append(numBBB).append("\t").append(numBBBRef).append("\t\t")
							.append(numAberrant).append("\t").append(numAberrantRef).append("\t\t").append(numOthers)
							.append("\t").append(numOthersRef).append("\t\t").append(numFused).append("\t")
							.append(numFusedRef).append("\t\t").append(numEscape).append("\t").append(numEscapeRef);
					fw.write(sb.toString());

					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}



	public SamplingPlot getPlot(int idx) {
		return (SamplingPlot) m_plots[idx];
	}


	public static class ViewFeatures {
		// View-organizational structures
		public static SimpleAdapter gridAdapter = null;
		public static SimpleAdapter gridAdapter2 = null;
		public static ArrayList<LinkedHashMap<String, String>> gridAdapterMap = null;


		/**
		 * Finds the mapping by the given id, if such a mapping exists already. Otherwise null is returned.
		 *
		 * 이러한 매핑이 이미 있는 경우 지정된 ID로 매핑을 찾습니다. 그렇지 않으면 null이 반환됩니다.
		 *
		 * @param id
		 * @return
		 */
		public static LinkedHashMap<String, String> findMapping(String id) {
			String value = null;
			for (LinkedHashMap<String, String> map : gridAdapterMap) {
				value = map.get("id");
				if (value != null && value.equals(id)) {
					return map;
				}
			}
			return null;
		}


		/**
		 * Initializes the mappings with the default entries.
		 *
		 * 기본 항목으로 매핑을 초기화합니다.
		 *
		 * @param con
		 */
		public static void init(Context con) {
			if (gridAdapterMap == null)
				gridAdapterMap = new ArrayList<LinkedHashMap<String, String>>(16);

			gridAdapter = new SimpleAdapter(con, gridAdapterMap, R.layout.feature_list_item, new String[]{"img", "value",
					"text", "sup", "sub"}, new int[]{R.id.imgItem, R.id.lblValue, R.id.lblText, R.id.lblValueSup,
					R.id.lblValueSub});

			gridAdapter2 = new SimpleAdapter(con,gridAdapterMap,R.layout.feature_list_item,new String[]{"value","text"}, new int[]{R.id.lblValue, R.id.lblText});

			modifyMapping("hr", "n/a", "n/a", "n/a", R.drawable.stat_notify_sync_error, "HR [bpm]");
			modifyMapping("rr", "n/a", "n/a", "n/a", R.drawable.stat_sys_warning, "RR [ms]");
			modifyMapping("qrsta", "n/a", "n/a", "n/a", R.drawable.stat_sys_warning, "QRSTA [mV]");
			modifyMapping("pvc", "n/a", "n/a", "n/a", R.drawable.stat_sys_warning, "PVCs [#]");
			modifyMapping("num", "n/a", "n/a", "n/a", R.drawable.stat_sys_warning, "Beats [#]");


			//modifyMapping2("data","n/a","DATA");
		}






		/**
		 * Modifies a mapping. Creating it if it doesn't exist. In that case all parameters MUST be non-null. If the
		 * mapping is just modified any parameter that is not changed should be set to <code>null</code>.
		 *
		 * 매핑을 수정합니다. 존재하지 않는 경우 생성합니다. 이 경우 모든 매개변수는 null이 아니어야 합니다(MUST).
		 * 매핑이 방금 수정된 경우 변경되지 않은 매개변수는 <code>null</code>로 설정해야 합니다.
		 *
		 * @param id
		 * @param value
		 * @param sup
		 * @param sub
		 * @param img
		 */
		public static void modifyMapping(String id, String value, String sup, String sub, Integer img, String text) {
			LinkedHashMap<String, String> map;
			map = findMapping(id);
			if (map == null) {
				// mapping doesn't exist, create it. We expect all objects are non-null.
				map = new LinkedHashMap<String, String>(8);
				map.put("id", id);
				map.put("img", img.toString());
				map.put("value", value);
				map.put("text", text);
				map.put("sup", sup);
				map.put("sub", sub);
				gridAdapterMap.add(map);
			} else {
				if (value != null)
					map.put("value", value);
				if (img != null)
					map.put("img", img.toString());
				if (text != null)
					map.put("text", text);
				if (sup != null)
					map.put("sup", sup);
				if (sub != null)
					map.put("sub", sub);
			}

			// notify adapter about the change
			if (gridAdapter != null)
				gridAdapter.notifyDataSetChanged();
		}

		public static void modifyMapping2(String id, String value, String text) {
			LinkedHashMap<String, String> map;
			map = findMapping(id);
			if(map == null) {
				map = new LinkedHashMap<String, String>(8);
				map.put("id", id);
				map.put("value", value);
				map.put("text",text);
				gridAdapterMap.add(map);
			} else {
				if(value != null)
					map.put("value",value);
				if (text != null)
					map.put("text", text);
			}
		}
	}




	/**
	 * Called when the activity is first created.
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public  void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// 앱 시작 시 폰 저장소에 저장활 수 있도록 허가 묻는 코드
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);



		/*  data/data/패키지/ 경로에 직접 files 폴더 만들기 -> 애초에 apk를 설치하는 걸로는 앱 패키지 경로가 만들어 지지 않음
	private void copyDatabase() {
		String DB_PATH = "/data/data/" + getApplicationContext().getPackageName() + "/files/";
		String DB_NAME = "mitdb210ann";
		String DB_NAME2 = "mitdb210sig";

		try{ // 디렉토리가 없으면, 디렉토리를 먼저 생성한다.
			 File fDir = new File( DB_PATH );
			 if( !fDir.exists() ) { fDir.mkdir(); }

			 String strOutFile = DB_PATH + DB_NAME;
			 InputStream inputStream = getApplicationContext().getAssets().open( DB_NAME );
			 OutputStream outputStream = new FileOutputStream( strOutFile );

			 byte[] mBuffer = new byte[1024];
			 int mLength;
			 while( ( mLength = inputStream.read( mBuffer) ) > 0 ) {
			 	outputStream.write( mBuffer, 0, mLength );
			 }

			 outputStream.flush();
			 outputStream.close();
			 inputStream.close();
		}catch( Exception e ) {
			e.printStackTrace();
		}
	}
	*/



		/*
		//내부저장소에 패키지명 폴더 생성   앱을 실행하면 폴더가 만들어졌다는 토스트는 뜨지만 어디에도 없음
		String dirPath = getFilesDir().getAbsolutePath();
		File file = new File(dirPath, "/de.lme.heartnhealth4u/files");
		// 일치하는 폴더가 없으면 생성
		if( !file.exists() ) {
			file.mkdirs();
			Toast.makeText(this, "Success Folder ", Toast.LENGTH_SHORT).show();
		}else {
			Toast.makeText(this, "Already Folder ", Toast.LENGTH_SHORT).show();
		}

		String testStr = "ABCDEFGHIJK...";
		File savefile = new File(dirPath+"/test.txt");
		try{
			FileOutputStream fos = new FileOutputStream(savefile);
			fos.write(testStr.getBytes());
			fos.close();
			Toast.makeText(this, "Save Success", Toast.LENGTH_SHORT).show();
		} catch(IOException e){

		}
		*/



		/*
		// 외부저장소 최상위 경로에 패키지명 폴더 생성   폴더는 생기지만 안에 데이터를 넣고 접근할 수 있게 맞춰줘도 반응이 없음
		File appDirectory = new File(Environment.getExternalStorageDirectory(), "/de.lme.heartnhealth4u/files");
		if (!appDirectory.exists()) {
			appDirectory.mkdirs();
			Toast.makeText(this, "Success Folder ", Toast.LENGTH_SHORT).show();
		}else {
			Toast.makeText(this, "Already Folder ", Toast.LENGTH_SHORT).show();
		}
		*/


		/*
		File directory = new File(Context.getExternalFilesDir(null),"de.lme.heartnhealth4u");
		if(!directory.mkdirs()) {
			Log.d("file", "Directory was created");
		}*/




		//copyDatabase();

		/*
		//앱 패키지 경로 확인 용 - 내부 저장소 위치 -> data -> data -> 패키지 이름 -> files
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + "buffer.txt",true));
			bw.write("Hi~!");
			bw.newLine();
			bw.close();
			Toast.makeText(this,"저장완료",Toast.LENGTH_SHORT).show();
		}catch(IOException e) {
			e.printStackTrace();
			Toast.makeText(this,e.getMessage(),Toast.LENGTH_SHORT).show();
		}
		*/



		getSupportActionBar().setDisplayShowHomeEnabled(true); // 타이틀바에 이미지 추가
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setIcon(R.drawable.h24u);        // 타이틀바에 이미지 추가



		mHandler = new Handler();

		// ==============================================
		// == GRID VIEW (Features)
		// ====>
		m_gridFeatures = (GridView) findViewById(R.id.gridFeatures);
		ViewFeatures.init(this);
		m_gridFeatures.setAdapter(ViewFeatures.gridAdapter);
		// <====
		// ==============================================


		if (m_plots[PLOT_LIVE] == null) {
			m_plots[PLOT_LIVE] = new SamplingPlot("ECG live", Plot.generatePlotPaint(2f, 255, 38, 126, 202),PlotStyle.LINE,
					250000);
			((SamplingPlot) m_plots[PLOT_LIVE]).setAxis("t", "s", 1f, "", "", 1f);


			m_plotFeat1 = new SamplingPlot("ECG PVC Feat", Plot.generatePlotPaint(2f, 255, 126, 202, 38), PlotStyle.LINE, 8192);
			m_plotFeat2 = new SamplingPlot("ECG PVC Feat", Plot.generatePlotPaint(2f, 215, 202, 38, 126), PlotStyle.LINE, 8192);


			m_plots[PLOT_PANTS_OUT] = new SamplingPlot("Filter Out", Plot.generatePlotPaint(2f, 192, 38, 126, 202),
					PlotStyle.LINE, 8192);
			((SamplingPlot) m_plots[PLOT_PANTS_OUT]).setAxis("t", "s", 1f, "", "", 1f);
		}


		if (m_plots[PLOT_QRS] == null) {
			m_plots[PLOT_QRS] = new SamplingPlot("Last QRS", Plot.generatePlotPaint(2f, 192, 38, 126, 202), PlotStyle.LINE,
					8192);

			((SamplingPlot) m_plots[PLOT_QRS]).setAxis("t", "s", 1f, "", "", 1f);

			m_plotFeat3 = new SamplingPlot("ECG Feat", Plot.generatePlotPaint(2f, 215, 202, 38, 126), PlotStyle.LINE, 8192);


			// m_plots[ 1 ] = new Plot2D( "X", Plot.generatePlotPaint( 2f, 255, 202, 126, 38 ), PlotStyle.POINT, 512 );
			// plotTest2d.setAxis( "t", "s", 1f, "a", "g", 1f );
			// plotTest2d.setViewport( 60, 5 );
			// m_plots[ 1 ].loadFromFile( this, this.getResources().openRawResource( R.raw.plot2dim ) );
		}

		if (m_plots[PLOT_BEATS] == null) {
			m_plots[PLOT_BEATS] = new SamplingPlot("Heart Rate", Plot.generatePlotPaint(2f, 192, 38, 126, 202),
					PlotStyle.LINE, 8192);

			((SamplingPlot) m_plots[PLOT_BEATS]).setAxis("t", "s", 1f, "", "", 1f);

		}


		m_plotSlots[PLOT_LIVE] = (PlotView) findViewById(R.id.plotSlot1);  		// main 화면에서 첫 번째 ECG LIVE
		m_plotSlots[PLOT_PANTS_OUT] = (PlotView) findViewById(R.id.plotSlot2);  // main 화면에서 두 번째 FILTER OUT
		m_plotSlots[PLOT_QRS] = (PlotView) findViewById(R.id.plotSlot3);		// main 화면에서 세 번째 LAST QRS
		m_plotSlots[PLOT_BEATS] = (PlotView) findViewById(R.id.plotSlot4);		// main 화면에서 네 번째 HEART RATE
		m_plotSlots[PLOT_NA1] = (PlotView) findViewById(R.id.plotSlot5);
		m_plotSlots[PLOT_NA2] = (PlotView) findViewById(R.id.plotSlot6);

		m_plotGroup.addView(m_plotSlots[PLOT_LIVE], false);
		m_plotGroup.addView(m_plotSlots[PLOT_PANTS_OUT], false);


		for (int i = 0; i < NUM_PLOT_SLOTS; ++i) {
			if (m_plots[i] != null) {
				m_plotSlots[i].setVisibility(View.VISIBLE);
				m_plotSlots[i].attachPlot(m_plots[i], null);
				m_plotSlots[i].setMaxRedrawRate(1);
			} else
				m_plotSlots[i].setVisibility(View.GONE);
		}

		m_plotSlots[PLOT_PANTS_OUT].attachPlot(m_plotFeat1, null);
		m_plotSlots[PLOT_PANTS_OUT].attachPlot(m_plotFeat2, null);

		m_plotSlots[PLOT_QRS].attachPlot(m_plotFeat3, null);

		m_plotSlots[PLOT_LIVE].setMaxRedrawRate(40);
		m_plotSlots[PLOT_PANTS_OUT].setMaxRedrawRate(40);
		m_plotSlots[PLOT_BEATS].setMaxRedrawRate(40);
		m_plotSlots[PLOT_QRS].setMaxRedrawRate(40);


		m_toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);

		updatePlots();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();  // 메뉴 xml을 만들고 실제 화면에 나타내서 사용하기 위해 객체를 메모리에 올리는 과정(팽창) - inflater
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}


	/**
	 * Disconnect the Shimmer service   Shimmer 서비스 연결 해제
	 */
	public void disconnect() {
		if (mIShimSimService != null) {
			Log.d(TAG, "연결해제");
			try {
				mIShimSimService.unregisterCallback(HeartyActivity.this);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.unbindService(mConnection);
			mIShimSimService = null;
			mConnection = null;
			m_isConnected = false;
		}
	}


	public static final int RESULT_FEATURE = 1;
	public static final int RESULT_PLOT = 2;


	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RESULT_FEATURE) {

		} else if (requestCode == RESULT_PLOT) {
			updatePlots();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	/**
	 * Update the visibility of all plotviews, depending on the settings
	 *
	 * 설정에 따라 모든 플롯뷰의 시작정보 업데이트
	 *
	 */
	public void updatePlots() {
		String pref;
		for (int i = 0; i < NUM_PLOT_SLOTS; ++i) {
			if (m_plotSlots[i] != null) {
				if (i == PLOT_PANTS_OUT) {
					pref = "pk_plot_pants";
				} else if (i == PLOT_QRS) {
					pref = "pk_plot_qrs";
				} else if (i == PLOT_BEATS) {
					pref = "pk_plot";
				} else {
					continue;
				}

				if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(pref, true)) {
					m_plotSlots[i].setEnabled(true);
					m_plotSlots[i].setVisibility(View.VISIBLE);
				} else {
					m_plotSlots[i].setEnabled(false);
					m_plotSlots[i].setVisibility(View.GONE);
				}
			}
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {  // 메뉴가 화면에 보여질 때 마다 호출하는 함수
		MenuItem item = menu.findItem(R.id.uplink);
		if (item != null) {
			if (m_isConnected)  // 초기값은  false
			{
				item.setChecked(true);
				item.setTitle(R.string.menuDisconnect);
				item.setIcon(R.drawable.ic_menu_block);
			} else {
				item.setChecked(false);
				item.setTitle(R.string.menuConnect);
				item.setIcon(R.drawable.ic_menu_refresh);
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {  // Menu Item을 선택하였을 때 호출되어 Item 객체가 넘어온다.
		// Handle item selection  기능을 다룰 항목명 선택
		switch (item.getItemId()) {
			case R.id.add_feat:  // Menu in Features
				startActivityForResult(new Intent(this, AttachFeatureActivity.class), RESULT_FEATURE);
				Toast.makeText(this, "특징", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.add_plot:  // Menu in Settings
				startActivityForResult(new Intent(this, de.lme.heartnhealth4u.PrefActivity.class), RESULT_PLOT);
				Toast.makeText(this, "설정", Toast.LENGTH_SHORT).show();
				return true;

			case R.id.test:  // Menu in Test Record ,앱 자체에 내장된 데이터를 이용하여 판별
				Toast.makeText(this, "테스트 데이터를 이용하여 동작", Toast.LENGTH_SHORT).show();
				m_con.dataSource = "test";  //m_con은 ShimConnection() 객체
				m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_INTERN;
				m_con.dataColumn = 2;
				m_con.dataMultiplier = 1;
				m_con.simCount = 0;

				// try to connect
				if (connectShim()) {
					Toast.makeText(this,
							"서비스에 연결되었습니다... 데이터를 로드하는 중입니다. 기다려주십시오. 데이터세트 크기에 따라 최대 1분이 소요될 수 있습니다.",
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "Error: Could not connect to ShimmerService!", Toast.LENGTH_LONG).show();
				}


				return true;

			case R.id.uplink:  // Menu in Connect , 패키지명 폴더의 files에 저장된 MIT-BIH 데이터를 이용하여 PVC룰 판별하기 위해 해당 파일에 연결
				               // 또는 Shimmer 장비를 이용하여 블투루스 연결된 상태에서 실시간 PVC를 측정하기 위해 사용
				Toast.makeText(this, "연결", Toast.LENGTH_SHORT).show();
				if (item.isChecked() || m_isConnected) // isChecked함수는 boolean형, m_isConnected의 초기값은 false
				{  // 메인 메뉴 Connect의 누르기전 기본 상태 - false
					disconnect();

					// show result dialog
					showDialog(DIALOG_RESULT);
				} else {  // Connect를 누르면 아래 코드 실행
					// use pref settings  기본 설정 사용(처음에는 아래 처럼 기본값으로 셋팅 되어 있음)
					// SharedPreferences는 앱의 데이터를 영속적으로 저장하기 위한 클래스
					// PreferenceManager.getDefaultSharedPreferences(Context context) 는 SharedPreferences 객체를 획득하기 위한 함수
					// s: key , s1: 값
					m_con.dataSource = PreferenceManager.getDefaultSharedPreferences(this).getString("pk_source", "blue");  // pk_source는 Data Source를 가리키고 blue는 블루투스를 가리킴
					m_con.dataColumn = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pk_col", "2"));
					m_con.dataMultiplier = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pk_mult", "1000"));
					m_con.simCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pk_simcount", "0"));

					if (m_con.dataSource.equals("blue")) {  // 데이터 소스 선택 항목에서 블투루스를 누르면 실행
						Toast.makeText(this, "Bluetooth", Toast.LENGTH_SHORT).show();
						m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_BLUETOOTH;
						m_con.dataSource = PreferenceManager.getDefaultSharedPreferences(this).getString("pk_bt_source", "default");
					} else if (m_con.dataSource.equals("simall")) {  // 데이터 소스 선택 항목에서 simall을 누르면 실행
						Toast.makeText(this, "Simulate All", Toast.LENGTH_SHORT).show();
						m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_SIMULATION;
						m_simAll = 8;
						nextSim();
						// m_con.dataSource = "100";
						// m_con.dataColumn = 2;
						// m_con.dataMultiplier = 1000;
						// m_con.simCount = 0;
						return true;
					} else if (m_con.dataSource.equals("test")) {  // 데이터 소스 선택 항목에서 test record를 누르면 실행
						Toast.makeText(this, "Test Record", Toast.LENGTH_SHORT).show();
						m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_INTERN;
					} else {  // MIT-BIH 데이터를 누르면 실행
						Toast.makeText(this, "MIT-BIH data", Toast.LENGTH_SHORT).show();
						m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService. SHIM_MODE_LIVE;
					}

					// m_con.simMode = ShimmerSimService.SHIM_MODE_SIMULATION;
					// m_con.dataSource = "848";
					// m_con.dataColumn = 3;
					// m_con.dataMultiplier = 1000;
					// m_con.simCount = 0;

					// try to connect
					if (connectShim()) {
						Toast.makeText(this,
								"서비스에 연결되었습니다... 데이터를 로드하는 중입니다. 기다려주십시오. 데이터세트 크기에 따라 최대 1분이 소요될 수 있습니다.",
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(this, "Error: Could not connect to ShimmerService!", Toast.LENGTH_LONG).show();
					}
				}

				return true;

			case R.id.sound:  // Menu in Disabled Sound
				Toast.makeText(this, "소리 설정", Toast.LENGTH_SHORT).show();
				if (item.isChecked()) {
					m_playSound = true;
					item.setChecked(false);
					item.setTitle(R.string.menuSoundDisable);
					item.setIcon(R.drawable.ic_menu_refresh);
				} else {
					m_playSound = false;
					item.setChecked(true);
					item.setTitle(R.string.menuSoundEnable);
					item.setIcon(R.drawable.ic_menu_block);
				}

				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		disconnect();
		super.onDestroy();

	}


	/* (non-Javadoc)
	 * @see android.os.IInterface#asBinder()
	 */
	public IBinder asBinder() {
		// === TODO Auto-generated method stub
		return null;
	}


	public class ShimServiceConnection implements ServiceConnection {
		public void onServiceDisconnected(ComponentName name) {
			Log.e(TAG, "서비스가 예기치 않게 연결 해제되었습니다.");
			mIShimSimService = null;
		}


		public void onServiceConnected(ComponentName name, IBinder service) {
			// === TODO Auto-generated method stub
			mIShimSimService = IShimmerSimService.Stub.asInterface(service);
			try {
				mIShimSimService.registerCallback(HeartyActivity.this);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	/**
	 * Connect to the shimmer service
	 *
	 * @return
	 */
	public boolean connectShim() {  // 메뉴 항목에서 연결을 눌렀을 때 작동을 나타내는 코드
		disconnect();
		mConnection = new ShimServiceConnection();

		if (this.bindService(new Intent(getApplicationContext(),ShimmerSimService.class )
										.putExtra( de.lme.heartnhealth4u.ShimmerSimService.INTENT_EXTRA_SHIM_MODE, m_con.simMode )
										.putExtra( de.lme.heartnhealth4u.ShimmerSimService.INTENT_EXTRA_DATA_SOURCE, m_con.dataSource )
										.putExtra( de.lme.heartnhealth4u.ShimmerSimService.INTENT_EXTRA_DATA_COL, m_con.dataColumn )
										.putExtra( de.lme.heartnhealth4u.ShimmerSimService.INTENT_EXTRA_DATA_MULT, m_con.dataMultiplier )
										.putExtra( de.lme.heartnhealth4u.ShimmerSimService.INTENT_EXTRA_DATA_COUNT, m_con.simCount ),
								mConnection,
								Context.BIND_AUTO_CREATE ))
		{
			m_isConnected = true;

			mHandler.post( new Runnable() {
				public void run ()
				{
					HeartyActivity.this.setTitle( "Heart & Health For You - " + m_con.dataSource );  // 상단에 표시되는 항목
				}
			} );
			return true;
		}
		return false;
	}


	private transient Paint	m_paintGood	= Plot.generatePlotPaint( Paint.Style.FILL_AND_STROKE, 128, 143, 188, 143 );
	private transient Paint	m_paintBad	= Plot.generatePlotPaint( Paint.Style.FILL_AND_STROKE, 128, 188, 143, 143 );

	private static int		m_simAll	= 0;


	/**
	 * Simulate next MIT record
	 */
	public void nextSim ()
	{
		disconnect();

		++m_simAll;

		String array[] = getResources().getStringArray( R.array.arrayDataSourcesInt );
		String carray[] = getResources().getStringArray( R.array.arrayDataSourcesCols );

		if (m_simAll < array.length)
		{
			m_con.simMode = de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_SIMULATION;

			// find next entry in the col_array that is valid
			// col_array에서 유효한 다음 항목을 찾습니다.
			m_con.dataColumn = Integer.parseInt( carray[ m_simAll ] );
			m_con.dataMultiplier = 1000;
			while (m_con.dataColumn == -1)
			{
				m_simAll++;
				if (m_simAll >= carray.length)
					return;
				m_con.dataColumn = Integer.parseInt( carray[ m_simAll ] );
			}
			if (m_con.dataColumn == -1)
			{
				// no entry left
				return;
			}
			if (m_con.dataColumn < 0)
			{
				m_con.dataMultiplier = -1000;
				m_con.dataColumn = -m_con.dataColumn;
			}

			m_con.dataSource = array[ m_simAll ];

			connectShim();
		}
	}


	/* (non-Javadoc)
	 * @see de.lme.hearty.IShimmerSimServiceCallback#onShimmerSimEvent(int, long, float, char)
	 */
	// ECG Live, Filter Out, Last QRS, Heart Rate 그래프 출력 관련 코드
	public void onShimmerSimEvent (int eventID, long timestamp, float sensorValue, char label) throws RemoteException
	{
		if (eventID == de.lme.heartnhealth4u.ShimmerSimService.SHIM_MESSAGE_INIT)
		{
			// init message
			m_samplingRate = (int) timestamp;
			m_pants = new de.lme.heartnhealth4u.PanTompkins( m_samplingRate );


			((SamplingPlot) m_plots[ PLOT_PANTS_OUT ]).clear();
			((SamplingPlot) m_plots[ PLOT_PANTS_OUT ]).setViewport( m_samplingRate, 1 );
			((SamplingPlot) m_plots[ PLOT_LIVE ]).clear();
			((SamplingPlot) m_plots[ PLOT_LIVE ]).setViewport( m_samplingRate, 1 );
			m_plotFeat1.clear();
			m_plotFeat1.setViewport( m_samplingRate, 1 );
			m_plotFeat2.clear();
			m_plotFeat2.setViewport( m_samplingRate, 1 );
			m_plotFeat3.clear();
			m_plotFeat3.setViewport( m_samplingRate, 1 );
			((SamplingPlot) m_plots[ PLOT_QRS ]).clear();
			((SamplingPlot) m_plots[ PLOT_QRS ]).setViewport( m_samplingRate, 1 );
			// ((SamplingPlot) m_plots[ PLOT_BEATS ]).setViewport( samplingRate, 1 );

			m_result = new SimResult( m_con.dataSource );
			return;
		}
		else if (eventID == de.lme.heartnhealth4u.ShimmerSimService.SHIM_MESSAGE_SIM_END)
		{
			// End of Record message
			m_result.save( this );

			if (m_simAll > 0)
			{
				nextSim();
			}
			else
			{
				mHandler.post( new Runnable() {
					public void run ()
					{
						// show results
						showDialog( DIALOG_RESULT );
					}
				} );
			}
			return;
		}

		// Default sensor event

		// Log.d( TAG, "event @ " + p_timestamp );
		if (m_plots[ PLOT_LIVE ] != null)
		{
			// draw raw input
			((SamplingPlot) m_plots[ PLOT_LIVE ]).addValue( sensorValue, timestamp );

			// processing pipeline
			m_pants.next( sensorValue, timestamp );

			// draw bandOut
			((SamplingPlot) m_plots[ PLOT_PANTS_OUT ]).addValue( (float) m_pants.y[ 3 ], timestamp );

			// draw intOut and intMean
			m_plotFeat1.addValue( (float) m_pants.y[ 6 ], timestamp );
			m_plotFeat2.addValue( (float) m_pants.qrsThreshold, timestamp );


			// if this beat is labeled, count it. But not if pants is still learning.
			if (m_result != null && m_pants.startProcessing <= 0)
			{
				m_result.newLabel( label );
			}


			if (QRS.qrsCurrent.segState == SegmentationStatus.FINISHED)
			{
				// if segState == FINISHED then a qrs complex was detected

				// plot both current template beats
				// PanTompkins.QRS.template1.drawBeat( m_plotFeat3 );
				// PanTompkins.QRS.template2.drawBeat( m_plotFeat3 );

				// Add some space to the QRS segmentation plot
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );
				((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( 0f, timestamp );

				// play heartbeat tone
				if (m_playSound)
					m_toneGen.startTone( ToneGenerator.TONE_DTMF_P, 100 );


				// plot last qrs , Last QRS
				if (QRS.qrsCurrent.values.num > 0)
				{
					for (int i = 0; i < QRS.qrsCurrent.values.num; ++i)
					{
						((SamplingPlot) m_plots[ PLOT_QRS ]).addValue( QRS.qrsCurrent.values.values[ i ], timestamp );

						// draw a circle marker for the Q-deflection
						// Q 편향에 대한 원 마커를 그립니다.
						if (i == QRS.qrsCurrent.qIdx)
							m_plots[ PLOT_QRS ].setMarker( new PlotMarkerDefault( DefaultMark.CIRCLE, null, 3f ) );

						if (i == QRS.qrsCurrent.rIdx)
						{
							// draw a circle marker
							m_plots[ PLOT_QRS ].setMarker( new PlotMarkerDefault( DefaultMark.CIRCLE, null, 5f ) );

							// draw a vertical line marker for the fiducial point (R-deflection)
							// 기준점에 대한 수직선 마커를 그립니다(R-편향) -> Filter Out의 긴 세로선?
							((SamplingPlot) m_plots[ PLOT_PANTS_OUT ])
									.setMarker( ((SamplingPlot) m_plots[ PLOT_PANTS_OUT ]).getValueHead()
											- QRS.qrsCurrent.values.num + i, new PlotMarkerDefault(
											DefaultMark.LINE_VERTICAL, null, 1f ) );
						}

						// draw a circle marker for the S-deflection
						if (i == QRS.qrsCurrent.sIdx)
							m_plots[ PLOT_QRS ].setMarker( new PlotMarkerDefault( DefaultMark.CIRCLE, null, 3f ) );

					}
				}
				else
				{
					// draw a vertical line for aberrant complexes
					((SamplingPlot) m_plots[ PLOT_PANTS_OUT ]).setMarker( ((SamplingPlot) m_plots[ PLOT_PANTS_OUT ])
							.getValueHead(), new PlotMarkerDefault( DefaultMark.LINE_VERTICAL, null, 1f ) );
				}


				// Inform result class about new beat
				m_result.newBeat( QRS.qrsCurrent );


				// draw an overlay area marker for normal or abnormal QRS complexes into the LIVE plot
				// ECG LIVE
				switch (QRS.qrsCurrent.classification)
				{
					case NORMAL:
						// mark normal QRS complex , 일반 QRS 컴플렉스 표시
						m_plots[ PLOT_LIVE ].setMarker( m_plots[ PLOT_LIVE ].getValueHead() - de.lme.heartnhealth4u.PanTompkins.TOTAL_DELAY
								- m_pants.maxQrsSize, new PlotMarkerDefault( DefaultMark.OVERLAY_BEGIN, m_paintGood, 5f ) );
						m_plots[ PLOT_LIVE ].setMarker( m_plots[ PLOT_LIVE ].getValueHead() - de.lme.heartnhealth4u.PanTompkins.TOTAL_DELAY,
														new PlotMarkerDefault( DefaultMark.OVERLAY_END, m_paintGood, 5f ) );
						break;

					default:
						// mark ectopic QRS , 이소성 QRS 표시
						m_plots[ PLOT_LIVE ].setMarker( m_plots[ PLOT_LIVE ].getValueHead() - de.lme.heartnhealth4u.PanTompkins.TOTAL_DELAY
								- m_pants.maxQrsSize, new PlotMarkerDefault( DefaultMark.OVERLAY_BEGIN, m_paintBad, 5f ) );
						m_plots[ PLOT_LIVE ].setMarker( m_plots[ PLOT_LIVE ].getValueHead() - de.lme.heartnhealth4u.PanTompkins.TOTAL_DELAY,
														new PlotMarkerDefault( DefaultMark.OVERLAY_END, m_paintBad, 5f ) );
						break;
				}


				// plot heart rate
				// Heart Rate
				if (m_plots[ PLOT_BEATS ] != null)
				{
					((SamplingPlot) m_plots[ PLOT_BEATS ]).addValue( (float) m_pants.heartRateStats.value, timestamp );
					((SamplingPlot) m_plots[ PLOT_BEATS ]).addValue( (float) m_pants.heartRateStats.value, timestamp );
					((SamplingPlot) m_plots[ PLOT_BEATS ]).addValue( (float) m_pants.heartRateStats.value, timestamp );
				}

				// update the text in the feature grid
				updateFeatureText();

				// inform pants that the beat has been processed
				QRS.qrsCurrent.segState = SegmentationStatus.PROCESSED;

				// only live-update plots if not in simulation mode
				if (m_con.simMode != de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_SIMULATION)
				{
					m_plotSlots[ PLOT_QRS ].requestRedraw( true );
					m_plotSlots[ PLOT_BEATS ].requestRedraw( true );
				}
			}

			if (m_con.simMode != de.lme.heartnhealth4u.ShimmerSimService.SHIM_MODE_SIMULATION)
			{
				m_plotSlots[ PLOT_LIVE ].requestRedraw( true );
				m_plotSlots[ PLOT_PANTS_OUT ].requestRedraw( true );
			}

			// Log.d( TAG, "event " + res + "  " + m_plot.values.minValue + "  " + m_plot.values.maxValue );
		}
	}


	/**
	 * updates the text in the feature grid
	 */
	public void updateFeatureText ()
	{
		mHandler.post( new Runnable() {
			public void run ()
			{
				ViewFeatures.modifyMapping( "hr",
											m_pants.heartRateStats.formatMean(),
											m_pants.heartRateStats.formatMax(),
											m_pants.heartRateStats.formatMin(),
											null,
											null );

				ViewFeatures.modifyMapping( "rr",
											m_pants.rrStats.formatValue(),
											m_pants.rrStats.formatMax(),
											m_pants.rrStats.formatMin(),
											null,
											null );

				ViewFeatures.modifyMapping( "qrsta",
											m_pants.qrstaStats.formatValue(),
											m_pants.qrstaStats.formatMax(),
											m_pants.qrstaStats.formatMin(),
											null,
											null );

				ViewFeatures.modifyMapping2("data",mBufferWritePos,null);


				if (m_result != null)
				{
					ViewFeatures.modifyMapping( "pvc",
												Integer.toString( m_result.numPvc + m_result.numAberrant ),
												Integer.toString( m_result.numPvcRef + m_result.numAberrantRef ),
												Integer.toString( m_result.numPvc + m_result.numAberrant - m_result.numPvcRef
														- m_result.numAberrantRef ),
												null,
												null );

					ViewFeatures.modifyMapping( "num",
												Integer.toString( m_result.numTotalBeats ),
												Integer.toString( m_result.numTotalBeatsRef ),
												Integer.toString( m_result.numTotalBeats - m_result.numTotalBeatsRef ),
												null,
												null );
				}
			}
		} );
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause ()
	{
		for (int i = 0; i < NUM_PLOT_SLOTS; ++i)
		{
			if (m_plotSlots[ i ] != null)
			{
				m_plotSlots[ i ].setEnabled( false );
			}
		}

		if (isFinishing())
		{
			m_toneGen.release();
			m_toneGen = null;
			disconnect();
		}
		super.onPause();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume ()
	{
		for (int i = 0; i < NUM_PLOT_SLOTS; ++i)
		{
			if (m_plotSlots[ i ] != null)
			{
				m_plotSlots[ i ].setEnabled( true );
			}
		}

		super.onResume();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed ()
	{
		showDialog( DIALOG_EXIT );
		// super.onBackPressed();
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog (int id)
	{
		Dialog dialog = null;
		if (id == DIALOG_EXIT)
		{
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( "나가기" ).setMessage( "정말 나가시겠습니까?" )
					.setPositiveButton( "네", new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							HeartyActivity.this.finish();
						}
					} ).setNegativeButton( "아니오", new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							// do nothing
						}
					} ).create();
		}
		else if (id == DIALOG_RESULT && m_result != null)
		{
			// exit dialog
			dialog = new AlertDialog.Builder( this ).setTitle( "Results" ).setMessage( m_result.format() )
					.setPositiveButton( "Ok", new DialogInterface.OnClickListener() {
						public void onClick (DialogInterface dialog, int which)
						{
							;
						}
					} ).create();
		}

		return dialog;
	}

}