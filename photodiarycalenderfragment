package com.example.aiplantbutlernew

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoDiaryCalendarFragment : Fragment() {

    // ── 저장 키 ─────────────────────────────────────────────────────────────────────
    private val prefsName = "diary_prefs"
    private val prefPhotosKey = "photo_map"
    private val reqPickImage = 4011

    // ── 상태 ───────────────────────────────────────────────────────────────────────
    private var currentMonth: java.util.Calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    private var selectedDayKey: Long = startOfDayKey(System.currentTimeMillis())

    // ── UI 레퍼런스 ────────────────────────────────────────────────────────────────
    private lateinit var tvMonth: TextView
    private lateinit var rvGrid: RecyclerView
    private lateinit var detailsContainer: LinearLayout   // 달력 아래 “날짜 + 메모” 미리보기

    // 날짜별 사진 캐시(캘린더 썸네일용) - Map 자체는 불변(val)이어도 내부 아이템은 변경 가능
    private val photoMap: MutableMap<Long, Uri> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        photoMap.putAll(loadPhotoMap(requireContext()))
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        // 스크롤 루트
        val rootScroll = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 하단바/FAB와 겹치지 않게 여백
            setPadding(0, 0, 0, dp(72))
        }
        val page = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.WHITE)
        }
        rootScroll.addView(page)

        // 상단 헤더
        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1DB954.toInt())
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = "Calendar"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        header.addView(title)
        page.addView(header)

        // 요일 헤더
        val dowRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach { s ->
            val t = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                setTextColor(0xFF8A8A8A.toInt())
                text = s
            }
            dowRow.addView(t)
        }
        page.addView(dowRow)

        // 월 이동 컨트롤
        val monthCtl = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val btnPrev = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            background = null
            contentDescription = "이전 달"
        }
        tvMonth = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            textSize = 16f
        }
        val btnNext = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_media_next)
            background = null
            contentDescription = "다음 달"
        }
        monthCtl.addView(btnPrev); monthCtl.addView(tvMonth); monthCtl.addView(btnNext)
        page.addView(monthCtl)

        // 달력 그리드
        rvGrid = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(420)
            ).apply { topMargin = dp(6) }
            layoutManager = GridLayoutManager(ctx, 7)
            isNestedScrollingEnabled = false
            setBackgroundColor(0xFFEFEFEF.toInt())
        }
        page.addView(rvGrid)

        // ── 달력 아래: 선택 날짜 미리보기(날짜 + 메모만) ──
        detailsContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
            visibility = View.GONE
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        page.addView(detailsContainer)

        // 바인딩
        fun refreshMonthUI() {
            tvMonth.text = SimpleDateFormat("yyyy년 M월", Locale.getDefault()).format(currentMonth.time)
            rvGrid.adapter = MonthAdapter(buildDaysOfMonth(currentMonth))
        }
        btnPrev.setOnClickListener { currentMonth.add(java.util.Calendar.MONTH, -1); refreshMonthUI(); renderAllMemos() }
        btnNext.setOnClickListener { currentMonth.add(java.util.Calendar.MONTH, 1); refreshMonthUI(); renderAllMemos() }

        refreshMonthUI()
        renderAllMemos() // 모든 메모를 날짜순으로 표시
        return rootScroll
    }

    // ── 날짜 탭 → 편집 바텀시트 ─────────────────────────────────────────────────────
    private fun onDateTapped(dayKey: Long) {
        selectedDayKey = dayKey
        showEditorBottomSheet(dayKey)
    }

    private fun showEditorBottomSheet(dayKey: Long) {
        val ctx = requireContext()
        val sheet = BottomSheetDialog(ctx)

        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }

        val tvDate = TextView(ctx).apply {
            text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayKey)
            textSize = 16f
        }
        wrap.addView(tvDate)

        // 선택된 사진 미리보기(시트 내부에서는 참고만, 달력 아래에는 표시하지 않음)
        val ivPreview = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(160)
            ).apply { topMargin = dp(12) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "사진 미리보기"
            val uri = loadPhotoUri(ctx, dayKey)
            if (uri != null) runCatching {
                ctx.contentResolver.openInputStream(uri).use { ins ->
                    val bmp = ins?.let { BitmapFactory.decodeStream(it) }
                    if (bmp != null) setImageBitmap(bmp)
                }
            }
        }
        wrap.addView(ivPreview)

        // 사진 선택/삭제
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        val btnPick = Button(ctx).apply {
            text = "사진 선택"
            setOnClickListener { openImagePicker(); sheet.dismiss() }
        }
        val btnDel = Button(ctx).apply {
            text = "사진 삭제"
            setOnClickListener {
                savePhotoUri(ctx, dayKey, null)
                photoMap.remove(dayKey)
                rvGrid.adapter?.notifyDataSetChanged()
                renderAllMemos()     // 아래 미리보기 갱신 (사진은 사용하지 않지만, 존재 여부로 표시/숨김 제어 가능)
                sheet.dismiss()
                Toast.makeText(ctx, "사진을 삭제했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(btnPick, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        row.addView(btnDel, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) })
        wrap.addView(row)

        // 메모 입력
        val etDiary = EditText(ctx).apply {
            hint = "일기를 입력하세요"
            gravity = Gravity.TOP or Gravity.START
            setLines(5)
            setText(loadDiaryText(ctx, dayKey))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        wrap.addView(etDiary)

        val btnSave = Button(ctx).apply {
            text = "저장"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            setOnClickListener {
                saveDiaryText(ctx, dayKey, etDiary.text?.toString() ?: "")
                renderAllMemos()     // ↓ 달력 아래 “날짜+메모” 갱신
                Toast.makeText(ctx, "저장되었습니다.", Toast.LENGTH_SHORT).show()
                sheet.dismiss()
            }
        }
        wrap.addView(btnSave)

        sheet.setContentView(wrap)
        sheet.show()
    }

    // ── 달력 아래 “모든 메모”를 날짜순으로 렌더링 ──────────────────────────────
    private fun renderAllMemos() {
        val ctx = requireContext()
        detailsContainer.removeAllViews()

        // prefs 전체에서 "text_" 로 저장된 메모만 모음
        val items: List<Pair<Long, String>> = prefs(ctx).all.mapNotNull { (k, v) ->
            if (k.startsWith("text_")) {
                val dayKey = k.removePrefix("text_").toLongOrNull()
                val memo = v as? String
                if (dayKey != null && !memo.isNullOrBlank()) dayKey to memo else null
            } else null
        }.sortedBy { it.first }

        if (items.isEmpty()) {
            detailsContainer.visibility = View.GONE
            return
        }
        detailsContainer.visibility = View.VISIBLE

        // 헤더(옵션)
        val header = TextView(ctx).apply {
            text = "메모"
            textSize = 16f
            setTextColor(0xFF333333.toInt())
        }
        detailsContainer.addView(header)

        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // 항목 렌더링
        items.forEachIndexed { index, (dayKey, memoText) ->
            if (index > 0) {
                // 간단한 구분선
                val divider = View(ctx).apply {
                    setBackgroundColor(0xFFE0E0E0.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
                    ).apply { topMargin = dp(8); bottomMargin = dp(8) }
                }
                detailsContainer.addView(divider)
            }

            val tvTitle = TextView(ctx).apply {
                text = df.format(java.util.Date(dayKey))
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            }
            detailsContainer.addView(tvTitle)

            val tvMemo = TextView(ctx).apply {
                this.text = memoText
                textSize = 14f
                setTextColor(0xFF444444.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            }
            detailsContainer.addView(tvMemo)
        }
    }

    // ── 결과 처리(사진 선택) ───────────────────────────────────────────────────────
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqPickImage && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            savePhotoUri(requireContext(), selectedDayKey, uri)
            photoMap[selectedDayKey] = uri
            rvGrid.adapter?.notifyDataSetChanged()
            // 아래 미리보기는 사진을 사용하지 않으므로 별도 갱신 불필요
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, reqPickImage)
    }

    // ── 월 그리드 데이터 ───────────────────────────────────────────────────────────
    private fun buildDaysOfMonth(monthCal: java.util.Calendar): List<Long?> {
        val temp = (monthCal.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }
        val leading = temp.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.SUNDAY
        val daysInMonth = temp.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Long?>()
        repeat(leading) { list.add(null) }
        repeat(daysInMonth) { d ->
            val c = temp.clone() as java.util.Calendar
            c.set(java.util.Calendar.DAY_OF_MONTH, d + 1)
            list.add(c.timeInMillis)
        }
        while (list.size % 7 != 0) list.add(null)
        return list
    }

    // ── 저장 유틸 ──────────────────────────────────────────────────────────────────
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private fun diaryTextKey(dayKey: Long) = "text_$dayKey"
    private fun saveDiaryText(ctx: Context, dayKey: Long, text: String) { prefs(ctx).edit().putString(diaryTextKey(dayKey), text).apply() }
    private fun loadDiaryText(ctx: Context, dayKey: Long): String = prefs(ctx).getString(diaryTextKey(dayKey), "") ?: ""

    private fun savePhotoUri(ctx: Context, dayKey: Long, uri: Uri?) {
        val set = (prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()).toMutableSet()
        val prefix = "$dayKey="
        set.removeAll { it.startsWith(prefix) }
        if (uri != null) set.add(prefix + uri.toString())
        prefs(ctx).edit().putStringSet(prefPhotosKey, set).apply()
    }
    private fun loadPhotoUri(ctx: Context, dayKey: Long): Uri? {
        val set = prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()
        val prefix = "$dayKey="
        val raw = set.firstOrNull { it.startsWith(prefix) } ?: return null
        return runCatching { Uri.parse(raw.substringAfter('=')) }.getOrNull()
    }
    private fun loadPhotoMap(ctx: Context): Map<Long, Uri> {
        val set = prefs(ctx).getStringSet(prefPhotosKey, emptySet()) ?: emptySet()
        val map = mutableMapOf<Long, Uri>()
        for (s in set) {
            val i = s.indexOf('=')
            if (i > 0) {
                val k = s.substring(0, i).toLongOrNull() ?: continue
                val u = runCatching { Uri.parse(s.substring(i + 1)) }.getOrNull() ?: continue
                map[k] = u
            }
        }
        return map
    }

    private fun startOfDayKey(millis: Long): Long {
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).toInt()

    // ── 어댑터(썸네일만, 탭 시 바텀시트) ───────────────────────────────────────────
    private inner class MonthAdapter(
        private val items: List<Long?>
    ) : RecyclerView.Adapter<MonthAdapter.VH>() {

        inner class VH(val root: LinearLayout, val tv: TextView, val iv: ImageView) :
            RecyclerView.ViewHolder(root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val cell = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(86))
                setPadding(dp(2), dp(2), dp(2), dp(2))
                setBackgroundColor(Color.WHITE)
            }
            val tv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                gravity = Gravity.START
                textSize = 12f
                setTextColor(0xFF333333.toInt())
            }
            val iv = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(56)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            cell.addView(tv); cell.addView(iv)
            return VH(cell, tv, iv)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val millis = items[position]
            if (millis == null) {
                holder.tv.text = ""
                holder.iv.setImageDrawable(null)
                holder.root.setOnClickListener(null)
                return
            }

            val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)

            holder.tv.text = day.toString()
            holder.tv.setTextColor(
                when (dow) {
                    java.util.Calendar.SUNDAY -> 0xFFD32F2F.toInt()
                    java.util.Calendar.SATURDAY -> 0xFF1976D2.toInt()
                    else -> 0xFF333333.toInt()
                }
            )

            val key = startOfDayKey(millis)

            // Use a new local name and never reassign it
            val thumbUri: Uri? = photoMap[key] ?: loadPhotoUri(requireContext(), key)

            if (thumbUri != null) {
                try {
                    holder.itemView.context.contentResolver.openInputStream(thumbUri).use { ins ->
                        val bmp = ins?.let { BitmapFactory.decodeStream(it) }
                        if (bmp != null) {
                            holder.iv.setImageBitmap(bmp)
                        } else {
                            holder.iv.setImageDrawable(null)
                        }
                    }
                } catch (_: Exception) {
                    holder.iv.setImageDrawable(null)
                }
            } else {
                holder.iv.setImageDrawable(null)
            }

            holder.root.setOnClickListener { onDateTapped(key) }
        }
    }
}
