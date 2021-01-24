package com.kn.chartscanvas

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_pie_chart.*
import java.util.*
import kotlin.collections.ArrayList

class PieChartActivity : AppCompatActivity() {

    var pieChart: PieChartView? = null
    var chartItemAdapter: ChartItemAdapter? = null
    var items: ArrayList<Item>? = null
    var colors: MutableList<Int>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pie_chart)
        pieChart = findViewById(R.id.pie_chart)
        colors = ArrayList()
        initColor()

        flAddItem.setOnClickListener { openDialog() }
        initRecyclerView()
        btnResetChart.setOnClickListener { resetChartAndList() }
    }

    private fun initRecyclerView() {
        items = ArrayList()
        rvPieChart.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvPieChart.itemAnimator = DefaultItemAnimator()
        chartItemAdapter = ChartItemAdapter(this, items!!)
        rvPieChart.adapter = chartItemAdapter
    }

    private fun resetChartAndList() {
        colors?.clear()
        initColor()
        items?.clear()
        chartItemAdapter?.notifyDataSetChanged()
        initRecyclerView()
        pieChart?.resetChart()
    }

    private fun initColor() {
        colors?.add(R.color.piechart_1)
        colors?.add(R.color.piechart_2)
        colors?.add(R.color.piechart_3)
        colors?.add(R.color.piechart_4)
        colors?.add(R.color.piechart_5)
        colors?.add(R.color.piechart_6)
        colors?.add(R.color.piechart_7)
        colors?.add(R.color.piechart_8)
        colors?.add(R.color.piechart_9)
        colors?.add(R.color.piechart_10)
    }

    private fun openDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val edtItemName: EditText = view.findViewById(R.id.edtItemName)
        val edtItemValue: EditText = view.findViewById(R.id.edtItemDes)
        val alertDialog: AlertDialog = builder.create()
        builder.setMessage("Add item")
            .setView(view)
            .setPositiveButton("OK") { dialogInterface, i ->
                if (edtItemName.text.toString()
                        .trim { it <= ' ' } != "" && edtItemValue.text.toString()
                        .trim { it <= ' ' } != ""
                ) {
                    val itemName =
                        edtItemName.text.toString().trim { it <= ' ' }
                    val itemValue =
                        edtItemValue.text.toString().trim { it <= ' ' }.toFloat()
                    pieChart?.showText = true
                    if (colors!!.isEmpty()) {
                        Toast.makeText(
                            this,
                            "Limit 10 item",
                            Toast.LENGTH_SHORT
                        ).show()
                        alertDialog.dismiss()
                    } else {
                        val randomizer = Random()
                        val randomPosition: Int = randomizer.nextInt(colors!!.size)
                        val randomColor =
                            resources.getColor(colors!![randomPosition])
                        val item =
                            Item(
                                itemName,
                                itemValue,
                                randomColor
                            )
                        Log.d("color", "pos: " + randomPosition + "color: " + randomColor)
                        items?.add(item)
                        chartItemAdapter?.notifyDataSetChanged()
                        pieChart?.addItem(item)
                        colors?.removeAt(randomPosition)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, i -> alertDialog.dismiss() }
        builder.show()
    }
}