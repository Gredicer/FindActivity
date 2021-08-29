import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.util.TreeClassChooserFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.search.GlobalSearchScope
import java.io.BufferedReader
import java.io.InputStreamReader


class FindActivity : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project!!

        val result = execCMD("/usr/local/bin/adb shell dumpsys activity activities | grep mResumedActivity")
        val activityPackage = result.split("/")[1].split(" ")[0]
        println(activityPackage)
        val splitResult = activityPackage.split(".")
        val activity = splitResult[splitResult.size - 1]
        println(activity)

        val result2 = execCMD("/usr/local/bin/adb shell dumpsys activity $activityPackage")
        var flag = 0
        val fragments: MutableList<String?> = ArrayList()
        val lines = result2.split("\n").reversed()
        for (line in lines) {
            if (line.contains("#") && flag == 0) flag++
            if (line.contains("Added")) flag++
            if (flag == 1 && line.trim().startsWith("#")) {
                fragments.add(line.trim().split("{")[0].split(" ")[1])
            }
        }
        fragments.reverse()
        fragments.remove("SupportRequestManagerFragment")
        println(fragments)


        fragments.add(0, activity)


        val scope: GlobalSearchScope = GlobalSearchScope.allScope(project)
        val chooser = TreeClassChooserFactory.getInstance(project)
            .createNoInnerClassesScopeChooser(
                "Choose Class to Move",
                scope,
                {
                    fragments.contains(it.name)
                },
                null
            )
        chooser.showDialog()
        if (chooser.selected != null) NavigationUtil.activateFileWithPsiElement(chooser.selected)
    }


    /**
     * Execute command in terminal
     * @param command String
     * @return String
     */
    private fun execCMD(command: String): String {
        val sb = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        } catch (e: Exception) {
            return e.toString()
        }
        return sb.toString()
    }


}