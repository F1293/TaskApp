package jp.techacademy.fumio.ueda.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.fumio.ueda.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };
    private ListView mListView;
    private TaskAdapter mTaskAdapter;
    private EditText mCategorySearch;
    String CSW;
    TextView emptyTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //mCategorySearch = (EditText)  findViewById(R.id.category_search_text);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCategorySearch = (EditText) findViewById(R.id.category_search_text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        //エンター押したら検索
        mCategorySearch.setOnKeyListener(new View.OnKeyListener() {
                                             @Override
                                             public boolean onKey(View v, int keyCode, KeyEvent event) {
                                                 reloadListView();
                                                 //リストビューを更新
                                                 return false;
                                             }
        });


        // Realmの設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        // ListViewの設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        // ListViewをタップしたときの処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 入力・編集する画面に遷移させる
                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);
            }
        });

        // ListViewを長押ししたときの処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // タスクを削除する

                final Task task = (Task) parent.getAdapter().getItem(position);

                // ダイアログを表示する
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id", task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendingIntent);
                        //アラームも消す

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }
//リロードした時に下記の動作
    private void reloadListView() {

        CSW = mCategorySearch.getText().toString();

        //検索欄に何も入っていない時はすべて表示させる。文字列の長さ０で判定
        if (CSW.length() == 0){
            // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
            //ここの処理でカテゴリ分けするように編集
            RealmResults<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
            // 上記の結果を、TaskList としてセットする
            mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        }else {
            //すべて表示
            RealmResults<Task> taskRealmResults = mRealm.where(Task.class).equalTo("category", CSW).findAllSorted("date", Sort.DESCENDING);
            // 上記の結果を、TaskList としてセットする
            mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));
        }
        // TaskのListView用のアダプタに渡す
        mListView.setAdapter(mTaskAdapter);
        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged();

        emptyTextView = (TextView)findViewById(R.id.emptyTextView);
        mListView.setEmptyView(emptyTextView);
        //↑該当するリストがないとき表示

        }


}