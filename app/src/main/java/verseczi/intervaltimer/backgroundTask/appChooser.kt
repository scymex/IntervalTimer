package verseczi.intervaltimer.backgroundTask

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import verseczi.intervaltimer.R
import verseczi.intervaltimer.data.Database
import java.util.*

class appChooser(private val mContext: Context, private val ivAppIcon: ImageView, private val tvAppName: TextView, private val tvAppPackageName: TextView) : AsyncTask<String, String, String>() {
    val db: Database = Database(mContext)
    val dialog: Dialog = Dialog(mContext)

    override fun doInBackground(vararg params: String?): String? {
        var pm: PackageManager = mContext.packageManager
        dialog.setContentView(R.layout.package_list)
        dialog.setTitle("Choose an app!")

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList: ArrayList<ApplicationInfo> = packages.filterTo(ArrayList()) { pm.getLaunchIntentForPackage(it.packageName) != null }
        Collections.sort(appList, ApplicationInfo.DisplayNameComparator(pm))

        var dialog_ListView = dialog.findViewById(R.id.package_list) as ListView
        val adapter = PackageAdapter(mContext, R.layout.package_list_row, appList)

        dialog_ListView.adapter = adapter
        dialog_ListView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            db.packageName = appList[position].packageName

            ivAppIcon.setImageDrawable(appList[position].loadIcon(pm))
            tvAppName.text = appList[position].loadLabel(pm)
            tvAppPackageName.text = db.packageName
            dialog.dismiss()
        }

        return null
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        dialog.show()
    }

    class PackageAdapter(context: Context, layoutResourceId: Int, data: List<ApplicationInfo>) : ArrayAdapter<ApplicationInfo>(context, layoutResourceId, data) {
        var package_list: List<ApplicationInfo> = data
        var pm: PackageManager = context.packageManager
        var _layoutResourceId = layoutResourceId

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row: View
            val appInfo = package_list[position]

            if (convertView == null)
                row = (context as Activity).layoutInflater.inflate(_layoutResourceId, parent, false)
            else
                row = convertView

            val appName = row.findViewById(R.id.app_name) as TextView
            val packageName = row.findViewById(R.id.package_name) as TextView
            val iconView = row.findViewById(R.id.app_icon) as ImageView

            appName.text = appInfo.loadLabel(pm)
            packageName.text = appInfo.packageName
            iconView.setImageDrawable(appInfo.loadIcon(pm))

            return row
        }
    }

}