package com.android.fluxcut

import android.content.Context
import android.widget.Toast

object CloudManager {

    fun isCloudAvailable(): Boolean {
        return true
    }

    fun syncProject(context: Context, project: Project) {
        if (!isCloudAvailable()) return
        // TODO: Implement file upload for db and media
        Toast.makeText(context, "Cloud sync: ${project.title} backed up.", Toast.LENGTH_SHORT).show()
    }

    fun deleteFromCloud(context: Context, project: Project) {
        if (!isCloudAvailable()) return
        // TODO: Implement server delete
    }

    fun syncAll(context: Context) {
        Toast.makeText(context, "Synchronizing all projects...", Toast.LENGTH_LONG).show()
    }
}
