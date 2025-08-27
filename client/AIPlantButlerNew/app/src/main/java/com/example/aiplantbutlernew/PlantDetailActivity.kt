package com.example.aiplantbutlernew

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent

/**
 * HomeFragment 에서 전달한 식물의 할 일을 편집하는 화면.
 * - Task 데이터는 HomeFragment.kt 의 데이터클래스를 그대로 사용
 * - 드래그로 순서 변경, 스와이프로 삭제
 * - 체크박스 토글로 isDone 변경
 * - 저장 시 업데이트된 Plant 를 JSON 으로 돌려보냄
 */
class PlantDetailActivity : AppCompatActivity() {

    companion object {
        private val ID_CHECKBOX = View.generateViewId()
        private val ID_TITLE = View.generateViewId()
        private val ID_TIME = View.generateViewId()
    }

    private var plantPosition: Int = -1
    private lateinit var plant: Plant

    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ---------- 인텐트 파라미터 복원 ----------
        plantPosition = intent.getIntExtra("plantPosition", -1)
        val plantJson = intent.getStringExtra("plantJson")
        if (plantPosition == -1 || plantJson == null) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        plant = Gson().fromJson(plantJson, Plant::class.java)
        if (plant.tasks == null) {
            plant = plant.copy(tasks = mutableListOf())
        }

        // ---------- 간단한 코드 UI ----------
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = TextView(this).apply {
            text = "${plant.name} 할 일"
            textSize = 20f
            setPadding(dp(16))
        }
        root.addView(header)

        val recycler = RecyclerView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            layoutManager = LinearLayoutManager(this@PlantDetailActivity)
        }
        root.addView(recycler)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12))
        }
        val etNew = EditText(this).apply {
            hint = "할 일을 입력하세요"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnAdd = Button(this).apply {
            text = "추가"
            setOnClickListener {
                val text = etNew.text?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    plant.tasks!!.add(Task(description = text, isDone = false, alarmTime = null))
                    adapter.notifyItemInserted(plant.tasks!!.size - 1)
                    etNew.setText("")
                }
            }
        }
        btnRow.addView(etNew)
        btnRow.addView(btnAdd, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = dp(8)
        })
        root.addView(btnRow)

        val btnSave = Button(this).apply {
            text = "저장"
            setOnClickListener { returnUpdatedPlant() }
        }
        root.addView(btnSave, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = dp(16); rightMargin = dp(16); bottomMargin = dp(16)
        })

        setContentView(root)

        // ---------- 어댑터/드래그 설정 ----------
        adapter = TaskAdapter(plant.tasks ?: mutableListOf())
        recycler.adapter = adapter

        val callback = ItemMoveCallback(object : ItemMoveCallback.ItemTouchHelperAdapter {
            override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
                val list = plant.tasks!!
                if (fromPosition in list.indices && toPosition in list.indices) {
                    val moved = list.removeAt(fromPosition)
                    list.add(toPosition, moved)
                    adapter.notifyItemMoved(fromPosition, toPosition)
                    return true
                }
                return false
            }

            override fun onItemDismiss(position: Int) {
                val list = plant.tasks!!
                if (position in list.indices) {
                    list.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
            }
        })
        ItemTouchHelper(callback).attachToRecyclerView(recycler)
    }

    override fun onBackPressed() {
        returnUpdatedPlant()
        super.onBackPressed()
    }

    private fun returnUpdatedPlant() {
        val result = intent.apply {
            putExtra("plantPosition", plantPosition)
            putExtra("plantJson", Gson().toJson(plant))
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()

    private fun scheduleAlarm(ctx: Context, triggerAt: Long, message: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            message.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun cancelAlarm(ctx: Context, message: String) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ctx, AlarmReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pi = PendingIntent.getBroadcast(
            ctx,
            message.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    // ----------------- RecyclerView Adapter -----------------
    private inner class TaskAdapter(
        private val items: MutableList<Task>
    ) : RecyclerView.Adapter<TaskAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val check: CheckBox = view.findViewById(ID_CHECKBOX)
            val title: TextView = view.findViewById(ID_TITLE)
            val btnTime: ImageButton = view.findViewById(ID_TIME)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(12))
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val cb = CheckBox(parent.context).apply { id = ID_CHECKBOX }
            val tv = TextView(parent.context).apply {
                id = ID_TITLE
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 16f
            }
            val time = ImageButton(parent.context).apply {
                id = ID_TIME
                setImageResource(android.R.drawable.ic_menu_recent_history)
                background = null
                contentDescription = "알람 시간 설정"
            }
            row.addView(cb)
            row.addView(tv)
            row.addView(time)
            return VH(row)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val task = items[position]

            holder.title.text = task.description
            holder.check.isChecked = task.isDone

            holder.check.setOnCheckedChangeListener(null)
            holder.check.setOnCheckedChangeListener { _, isChecked ->
                task.isDone = isChecked
                val idx = holder.adapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    notifyItemChanged(idx)
                }
                if (!isChecked) {
                    cancelAlarm(this@PlantDetailActivity, task.description)
                }
            }

            holder.itemView.setOnClickListener {
                // 간단한 수정 다이얼로그
                val input = EditText(this@PlantDetailActivity).apply { setText(task.description) }
                android.app.AlertDialog.Builder(this@PlantDetailActivity)
                    .setTitle("할 일 수정")
                    .setView(input)
                    .setPositiveButton("저장") { _, _ ->
                        val t = input.text?.toString()?.trim().orEmpty()
                        if (t.isNotEmpty()) {
                            val idx = holder.adapterPosition
                            if (idx != RecyclerView.NO_POSITION) {
                                // description은 Task의 val 이므로 새 객체로 치환
                                items[idx] = task.copy(description = t)
                                notifyItemChanged(idx)
                            }
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }

            holder.btnTime.setOnClickListener {
                // 시각 선택 다이얼로그 (오늘 날짜의 해당 시각으로 저장)
                val now = Calendar.getInstance()
                TimePickerDialog(this@PlantDetailActivity,
                    { _, hour, minute ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        task.alarmTime = cal.timeInMillis
                        scheduleAlarm(this@PlantDetailActivity, cal.timeInMillis, task.description)
                        Toast.makeText(this@PlantDetailActivity, "알람을 ${hour}시 ${minute}분으로 설정했어요.", Toast.LENGTH_SHORT).show()
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            }
        }
    }
}