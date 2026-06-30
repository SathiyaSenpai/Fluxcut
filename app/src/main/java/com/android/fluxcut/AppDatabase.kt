package com.android.fluxcut

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val date: String,
    val duration: String,
    val resolution: String,
    val aspectRatio: String,
    val fps: Int,
    val thumbnailColorArgb: Int,
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "clips",
    primaryKeys = ["id", "projectId"],
    foreignKeys = [
        ForeignKey(
            entity        = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns  = ["projectId"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class ClipEntity(
    val id: Int,
    val projectId: Int,
    val name: String,
    val track: String,
    val startMs: Long,
    val durationMs: Long,
    val trimStartMs: Long,
    val colorArgb: Int,
    val hasAudio: Boolean,
    val isImage: Boolean,
    val mimeType: String?,
    val sourceUri: String?
)

data class ProjectWithClips(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val clips: List<ClipEntity>
)

data class ProjectWithThumbnailQuery(
    @Embedded val project: ProjectEntity,
    @ColumnInfo(name = "firstClipUri") val firstClipUri: String?,
    @ColumnInfo(name = "firstClipMimeType") val firstClipMimeType: String?
)

@Dao
interface ProjectDao {
    @Query("""
        SELECT p.*, 
        (SELECT sourceUri FROM clips WHERE projectId = p.id AND (track = 'VIDEO') ORDER BY startMs ASC LIMIT 1) as firstClipUri,
        (SELECT mimeType FROM clips WHERE projectId = p.id AND (track = 'VIDEO') ORDER BY startMs ASC LIMIT 1) as firstClipMimeType
        FROM projects p
        ORDER BY lastModified DESC
    """)
    fun observeAllWithThumbnail(): Flow<List<ProjectWithThumbnailQuery>>

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    suspend fun getAll(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project : ProjectEntity): Long

    @Delete
    suspend fun delete(project: ProjectEntity): Int

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Int): Int
}

@Dao
interface ClipDao {
    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectWithClips(projectId: Int): Flow<ProjectWithClips?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectWithClips(projectId: Int): ProjectWithClips?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClip(clip: ClipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClips(clips: List<ClipEntity>): List<Long>

    @Transaction
    suspend fun replaceClips(projectId: Int, clips: List<ClipEntity>) {
        deleteClipsByProject(projectId)
        upsertClips(clips)
    }

    @Query("DELETE FROM clips WHERE projectId = :projectId")
    suspend fun deleteClipsByProject(projectId: Int): Int

    @Query("DELETE FROM clips WHERE id = :clipId")
    suspend fun deleteClipById(clipId: Int): Int
}

@Database(
    entities  = [ProjectEntity::class, ClipEntity::class],
    version   = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fluxcut.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

fun ProjectEntity.toProject(thumbnailUri: String? = null, thumbnailMimeType: String? = null) = Project(
    id             = id,
    title          = title,
    date           = date,
    duration       = duration,
    resolution     = resolution,
    aspectRatio    = aspectRatio,
    fps            = fps,
    thumbnailColor = Color(thumbnailColorArgb),
    thumbnailUri   = thumbnailUri,
    thumbnailMimeType = thumbnailMimeType,
    lastModified   = lastModified
)

fun Project.toEntity() = ProjectEntity(
    id                  = id,
    title               = title,
    date                = date,
    duration            = duration,
    resolution          = resolution,
    aspectRatio         = aspectRatio,
    fps                 = fps,
    thumbnailColorArgb  = thumbnailColor.toArgb(),
    lastModified        = lastModified
)

fun ClipEntity.toTimelineClip() = TimelineClip(
    id           = id,
    name         = name,
    track        = TrackType.valueOf(track),
    startMs      = startMs,
    durationMs   = durationMs,
    trimStartMs  = trimStartMs,
    color        = Color(colorArgb),
    hasAudio     = hasAudio,
    isImage      = isImage,
    mimeType     = mimeType,
    sourceUri    = sourceUri
)

fun TimelineClip.toEntity(projectId: Int) = ClipEntity(
    id          = id,
    projectId   = projectId,
    name        = name,
    track       = track.name,
    startMs     = startMs,
    durationMs  = durationMs,
    trimStartMs = trimStartMs,
    colorArgb   = color.toArgb(),
    hasAudio    = hasAudio,
    isImage     = isImage,
    mimeType    = mimeType,
    sourceUri   = sourceUri
)

class ProjectRepository(context: Context) {
    private val db         = AppDatabase.get(context)
    private val dao        = db.projectDao()
    private val clipDao    = db.clipDao()

    val projects: Flow<List<Project>> = dao.observeAllWithThumbnail().map { list ->
        list.map { it.project.toProject(it.firstClipUri, it.firstClipMimeType) }
    }

    suspend fun save(project: Project) = dao.upsert(project.toEntity())

    suspend fun delete(project: Project) = dao.deleteById(project.id)

    fun observeProjectWithClips(projectId: Int): Flow<ProjectWithClips?> =
        clipDao.observeProjectWithClips(projectId)

    suspend fun getProjectWithClips(projectId: Int): ProjectWithClips? =
        clipDao.getProjectWithClips(projectId)

    suspend fun saveTimeline(project: Project, clips: List<TimelineClip>) {
        dao.upsert(project.toEntity())
        clipDao.replaceClips(project.id, clips.map { it.toEntity(project.id) })
    }

    /**
     * Creates a copy of [project], including all its clips, under a new id.
     * Clip ids are reused as-is since the (id, projectId) pair is what Room
     * treats as unique, so no clip-id remapping is needed.
     */
    suspend fun duplicate(project: Project): Project {
        val newProject = project.copy(
            id           = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            title        = "${project.title} Copy",
            lastModified = System.currentTimeMillis()
        )
        val originalClips = getProjectWithClips(project.id)?.clips?.map { it.toTimelineClip() } ?: emptyList()
        saveTimeline(newProject, originalClips)
        return newProject
    }
}