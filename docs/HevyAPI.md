# Hevy API Documentation

**Base URL:** `https://api.hevyapp.com`

## Authentication

All API requests require an API key to be passed in the HTTP header.

  * **Header Name:** `api-key`
  * **Value:** Your personal API key (available in Hevy Settings \> Developer).

-----

## Workouts

### 1\. Get Workouts

**Endpoint Header:** `GET /v1/workouts`

**Description:**
Retrieves a paginated list of the user's past workouts. The results include high-level details, exercise summaries, and volume metrics.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `page` | integer | Query | No | The page number to retrieve (default: 1). |
| `pageSize` | integer | Query | No | The number of items per page (default: 10). |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/workouts?page=1&pageSize=5" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "page": 1,
  "page_count": 5,
  "workouts": [
    {
      "id": "7823478-2342-2342-234234234",
      "title": "Upper Body Power",
      "description": "Focus on compound movements",
      "start_time": "2023-10-27T17:30:00Z",
      "end_time": "2023-10-27T18:45:00Z",
      "volume_kg": 12500,
      "exercise_count": 6,
      "exercises": [
        {
          "title": "Bench Press (Barbell)",
          "exercise_template_id": "3842394",
          "set_count": 4
        },
        {
          "title": "Pull Up",
          "exercise_template_id": "8472834",
          "set_count": 3
        }
      ]
    }
  ]
}
```

**Use Cases:**

1.  **Dashboard Display:** Fetching the last 5 workouts to display on a user's personal fitness dashboard or website.
2.  **Weekly Analysis:** Retrieving all workouts from the current week to calculate total volume or time spent training.
3.  **Data Export:** Looping through pages to back up all workout history to a local database.

-----

### 2\. Create Workout

**Endpoint Header:** `POST /v1/workouts`

**Description:**
Creates a new workout log. This is useful for importing workouts from other apps or programmatically logging sessions.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `workout` | object | Body | **Yes** | The workout object containing title, times, and exercises. |

**Sample Usage:**

```bash
curl -X POST "https://api.hevyapp.com/v1/workouts" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "workout": {
      "title": "Chest Day",
      "start_time": "2023-11-01T10:00:00Z",
      "end_time": "2023-11-01T11:00:00Z",
      "exercises": [
        {
          "exercise_template_id": "3842394",
          "sets": [
            { "type": "normal", "weight_kg": 80, "reps": 8, "rpe": 9 }
          ]
        }
      ]
    }
  }'
```

**Sample Response:**

```json
{
  "id": "99887766-5544-3322-1100-aabbccddeeff",
  "title": "Chest Day",
  "status": "created"
}
```

**Use Cases:**

1.  **Migration Tool:** Importing workout history from a CSV export of a different fitness app (e.g., Strong, Fitbod).
2.  **Smart Equipment Sync:** Automatically logging a workout when a user finishes a session on a smart home gym device.

-----

### 3\. Get Workout Count

**Endpoint Header:** `GET /v1/workouts/count`

**Description:**
Returns the total number of workouts logged by the user.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/workouts/count" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "workout_count": 342
}
```

**Use Cases:**

1.  **Gamification:** Displaying a "Total Workouts" badge on a personal website.
2.  **Pagination Calculation:** Determining the total number of pages required to fetch all workout history.

-----

### 4\. Get Workout Events

**Endpoint Header:** `GET /v1/workouts/events`

**Description:**
Retrieves a paged list of workout "events" (creations, updates, or deletions) since a specific date. This is designed for syncing local databases without re-fetching the entire workout history.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `page` | integer | Query | No | Page number (default: 1). |
| `pageSize` | integer | Query | No | Items per page (default: 10). |
| `since` | string | Query | **Yes** | ISO 8601 Date string (e.g., `2023-01-01T00:00:00Z`). |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/workouts/events?page=1&pageSize=20&since=2023-12-01T00:00:00Z" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "page": 1,
  "events": [
    {
      "id": "evt_998877",
      "type": "created",
      "workout_id": "7823478-2342-2342",
      "timestamp": "2023-12-02T08:30:00Z"
    },
    {
      "id": "evt_998878",
      "type": "deleted",
      "workout_id": "112233-4455-6677",
      "timestamp": "2023-12-03T09:00:00Z"
    }
  ]
}
```

**Use Cases:**

1.  **Syncing:** A desktop app checks for any changes made on the mobile app since the last login to update its local cache.
2.  **Audit Log:** Tracking how often workouts are modified or deleted.

-----

### 5\. Get Single Workout

**Endpoint Header:** `GET /v1/workouts/{workoutId}`

**Description:**
Retrieves the complete details of a specific workout, including every set, rep, weight, and RPE (Rate of Perceived Exertion) logged.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `workoutId` | string | Path | **Yes** | The unique UUID of the workout. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/workouts/7823478-2342-2342-234234234" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "id": "7823478-2342-2342-234234234",
  "title": "Leg Day",
  "start_time": "2023-10-25T09:00:00Z",
  "end_time": "2023-10-25T10:15:00Z",
  "exercises": [
    {
      "index": 0,
      "title": "Squat (Barbell)",
      "exercise_template_id": "A29384",
      "notes": "Felt strong today",
      "sets": [
        {
          "index": 0,
          "type": "warmup",
          "weight_kg": 60,
          "reps": 10,
          "rpe": null
        },
        {
          "index": 1,
          "type": "normal",
          "weight_kg": 100,
          "reps": 5,
          "rpe": 8
        },
        {
          "index": 2,
          "type": "normal",
          "weight_kg": 100,
          "reps": 5,
          "rpe": 8.5
        }
      ]
    }
  ]
}
```

**Use Cases:**

1.  **Detailed Review:** A user clicking on a specific workout in a list to see exactly what they lifted.
2.  **Coach Analysis:** A coach reviewing a client's specific session to provide feedback on load selection.

-----

### 6\. Update Workout

**Endpoint Header:** `PUT /v1/workouts/{workoutId}`

**Description:**
Updates an existing workout log. This is used to correct mistakes, add missed sets, or modify notes after a workout has been completed.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `workoutId` | string | Path | **Yes** | The UUID of the workout to update. |
| `workout` | object | Body | **Yes** | The updated workout object (must include the full structure). |

**Sample Usage:**

```bash
curl -X PUT "https://api.hevyapp.com/v1/workouts/7823478-2342-2342-234234234" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "workout": {
      "title": "Leg Day - Modified",
      "description": "Updated reps",
      "start_time": "2023-10-25T09:00:00Z",
      "end_time": "2023-10-25T10:15:00Z",
      "exercises": [
        {
          "exercise_template_id": "A29384",
          "sets": [
            { "index": 0, "type": "normal", "weight_kg": 105, "reps": 5, "rpe": 9 }
          ]
        }
      ]
    }
  }'
```

**Sample Response:**

```json
{
  "id": "7823478-2342-2342-234234234",
  "title": "Leg Day - Modified",
  "updated_at": "2023-10-26T14:20:00Z"
}
```

**Use Cases:**

1.  **Correction:** A user realizes they logged 100kg instead of 105kg and edits the entry via a third-party dashboard.
2.  **Enrichment:** Adding heart rate data or detailed notes to a workout after it has been synced from a wearable device.

-----

## Routines

### 7\. Get Routines

**Endpoint Header:** `GET /v1/routines`

**Description:**
Retrieves a paginated list of saved routines (workout templates).

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `page` | integer | Query | No | Page number. |
| `pageSize` | integer | Query | No | Items per page. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/routines?page=1" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "page": 1,
  "page_count": 2,
  "routines": [
    {
      "id": "rt_123456789",
      "title": "Push A",
      "folder_id": 1,
      "exercise_count": 5,
      "created_at": "2023-01-15T12:00:00Z"
    },
    {
      "id": "rt_987654321",
      "title": "Pull B",
      "folder_id": 1,
      "exercise_count": 4,
      "created_at": "2023-01-18T12:00:00Z"
    }
  ]
}
```

**Use Cases:**

1.  **Routine Management:** Displaying a list of available routines to a user in a custom web interface.
2.  **Analysis:** Comparing the volume or frequency of different routines (e.g., how often "Push A" is performed vs "Push B").

-----

### 8\. Create Routine

**Endpoint Header:** `POST /v1/routines`

**Description:**
Creates a new routine (workout template). Routines differ from workouts as they are plans to be followed, not historical logs of completed activity.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `routine` | object | Body | **Yes** | The routine object containing title and planned exercises. |

**Sample Usage:**

```bash
curl -X POST "https://api.hevyapp.com/v1/routines" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "routine": {
      "title": "Full Body Hypertrophy",
      "folder_id": null,
      "exercises": [
        {
          "exercise_template_id": "3842394",
          "sets": [
            { "type": "normal", "weight_kg": 80, "reps": 10 }
          ]
        }
      ]
    }
  }'
```

**Sample Response:**

```json
{
  "id": "rt_55667788",
  "title": "Full Body Hypertrophy",
  "exercise_count": 1
}
```

**Use Cases:**

1.  **Program Import:** Generating a 12-week program from a spreadsheet and uploading the routines into Hevy.
2.  **Sharing:** Copying a popular influencer's routine into a user's account.

-----

### 9\. Get Single Routine

**Endpoint Header:** `GET /v1/routines/{routineId}`

**Description:**
Retrieves the full details of a specific routine, including the target sets, reps, and weights.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `routineId` | string | Path | **Yes** | The unique ID of the routine. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/routines/rt_55667788" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "id": "rt_55667788",
  "title": "Full Body Hypertrophy",
  "folder_id": null,
  "exercises": [
    {
      "title": "Bench Press",
      "exercise_template_id": "3842394",
      "sets": [
        { "index": 0, "type": "normal", "weight_kg": 80, "reps": 10 }
      ]
    }
  ]
}
```

**Use Cases:**

1.  **Preview:** Displaying the contents of a routine before a user decides to start it.
2.  **Editing:** Fetching the current state of a routine to populate a form for editing.

-----

### 10\. Update Routine

**Endpoint Header:** `PUT /v1/routines/{routineId}`

**Description:**
Updates an existing routine's title, exercises, or target metrics.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `routineId` | string | Path | **Yes** | The ID of the routine to update. |
| `routine` | object | Body | **Yes** | The updated routine object. |

**Sample Usage:**

```bash
curl -X PUT "https://api.hevyapp.com/v1/routines/rt_55667788" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "routine": {
      "title": "Full Body Hypertrophy V2",
      "exercises": [...]
    }
  }'
```

**Sample Response:**

```json
{
  "id": "rt_55667788",
  "title": "Full Body Hypertrophy V2",
  "updated_at": "2023-11-05T10:00:00Z"
}
```

**Use Cases:**

1.  **Progression:** Updating the target weight for a routine as the user gets stronger.
2.  **Exercise Swap:** Replacing an exercise in a routine due to equipment unavailability.

-----

## ExerciseTemplates

### 11\. Get Exercise Templates

**Endpoint Header:** `GET /v1/exercise_templates`

**Description:**
Retrieves a list of available exercise templates (e.g., "Bench Press", "Squat", custom exercises). This is essential for finding the `exercise_template_id` needed to create or analyze workouts.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `page` | integer | Query | No | Page number. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/exercise_templates?page=1" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "page": 1,
  "exercise_templates": [
    {
      "id": "3842394",
      "title": "Bench Press (Barbell)",
      "type": "weight_reps",
      "primary_muscle_group": "Chest",
      "is_custom": false
    },
    {
      "id": "cus_998877",
      "title": "My Special Abs Circuit",
      "type": "duration",
      "primary_muscle_group": "Abs",
      "is_custom": true
    }
  ]
}
```

**Use Cases:**

1.  **Mapping Data:** Building a dictionary to map `exercise_template_id`s found in workout logs to human-readable names.
2.  **Custom Exercise Sync:** Checking if a custom exercise exists before attempting to create a new one.

-----

### 12\. Create Custom Exercise

**Endpoint Header:** `POST /v1/exercise_templates`

**Description:**
Creates a new custom exercise template. This is useful for users who perform niche exercises not found in the standard Hevy database.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `exercise_template` | object | Body | **Yes** | Object defining the exercise name, type, and muscles. |

**Sample Usage:**

```bash
curl -X POST "https://api.hevyapp.com/v1/exercise_templates" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "exercise_template": {
      "title": "Sandbag Carry",
      "type": "duration_weight",
      "primary_muscle_group": "Full Body"
    }
  }'
```

**Sample Response:**

```json
{
  "id": "cus_778899",
  "title": "Sandbag Carry",
  "is_custom": true
}
```

**Use Cases:**

1.  **Niche Sports:** Adding Strongman or CrossFit specific movements (e.g., Atlas Stones).
2.  **Rehab:** Creating specific rehabilitation movements prescribed by a physical therapist.

-----

### 13\. Get Single Exercise Template

**Endpoint Header:** `GET /v1/exercise_templates/{exerciseTemplateId}`

**Description:**
Retrieves detailed information about a specific exercise, including its type (weight & reps, duration, etc.) and primary muscle group.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `exerciseTemplateId` | string | Path | **Yes** | The ID of the exercise template. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/exercise_templates/3842394" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "id": "3842394",
  "title": "Bench Press (Barbell)",
  "type": "weight_reps",
  "primary_muscle_group": "Chest",
  "secondary_muscle_groups": ["Triceps", "Shoulders"],
  "is_custom": false
}
```

**Use Cases:**

1.  **Information Display:** Showing muscle group involvement for a specific exercise in an analytics dashboard.
2.  **Validation:** Verifying that an exercise ID corresponds to the expected movement.

-----

## RoutineFolders

### 14\. Get Routine Folders

**Endpoint Header:** `GET /v1/routine_folders`

**Description:**
Retrieves a list of folders used to organize routines.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `page` | integer | Query | No | Page number. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/routine_folders" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "page": 1,
  "folders": [
    {
      "id": 101,
      "title": "PPL Split",
      "created_at": "2023-01-01T12:00:00Z"
    },
    {
      "id": 102,
      "title": "Cardio",
      "created_at": "2023-01-02T12:00:00Z"
    }
  ]
}
```

**Use Cases:**

1.  **Organization:** Fetching folder IDs to properly categorize a new routine being created via API.
2.  **Navigation:** Building a folder-based navigation menu for routines in a custom app.

-----

### 15\. Create Routine Folder

**Endpoint Header:** `POST /v1/routine_folders`

**Description:**
Creates a new folder for organizing routines.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `folder` | object | Body | **Yes** | Object containing the folder title. |

**Sample Usage:**

```bash
curl -X POST "https://api.hevyapp.com/v1/routine_folders" \
  -H "Content-Type: application/json" \
  -H "api-key: YOUR_API_KEY_HERE" \
  -d '{
    "folder": {
      "title": "Summer Cut 2024"
    }
  }'
```

**Sample Response:**

```json
{
  "id": 103,
  "title": "Summer Cut 2024",
  "index": 0
}
```

**Use Cases:**

1.  **Batch Import:** Creating a specific folder to hold a newly imported set of routines (e.g., "Imported from Strong").
2.  **Program Setup:** Automatically creating a folder structure for a new training block.

-----

### 16\. Get Single Routine Folder

**Endpoint Header:** `GET /v1/routine_folders/{folderId}`

**Description:**
Retrieves the details of a specific routine folder using its ID. This is useful for validating that a folder exists or retrieving its current title and index before performing operations involving it.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `folderId` | integer | Path | **Yes** | The unique ID of the folder (e.g., `101`). |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/routine_folders/101" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "id": 101,
  "index": 0,
  "title": "PPL Split",
  "created_at": "2023-01-01T12:00:00Z",
  "updated_at": "2023-01-05T08:30:00Z"
}
```

**Use Cases:**

1.  **Validation:** Verifying that a specific folder ID stored in a user's local settings is still valid on the server.
2.  **Context Loading:** Fetching the title of a folder to display at the top of a UI when a user navigates into it.

-----

## ExerciseHistory

### 17\. Get Exercise History

**Endpoint Header:** `GET /v1/exercise_history/{exerciseTemplateId}`

**Description:**
Retrieves the historical performance data for a specific exercise across all workouts.

**Request Parameters:**

| Parameter | Type | In | Required | Description |
| :--- | :--- | :--- | :--- | :--- |
| `api-key` | string | Header | **Yes** | User authentication key. |
| `exerciseTemplateId` | string | Path | **Yes** | The ID of the exercise to analyze. |

**Sample Usage:**

```bash
curl -X GET "https://api.hevyapp.com/v1/exercise_history/3842394" \
  -H "accept: application/json" \
  -H "api-key: YOUR_API_KEY_HERE"
```

**Sample Response:**

```json
{
  "exercise_id": "3842394",
  "history": [
    {
      "workout_id": "7823478-2342-2342",
      "date": "2023-10-27T17:30:00Z",
      "one_rep_max": 100,
      "best_set": { "weight_kg": 90, "reps": 5 }
    },
    {
      "workout_id": "112233-4455-6677",
      "date": "2023-10-20T17:30:00Z",
      "one_rep_max": 95,
      "best_set": { "weight_kg": 85, "reps": 5 }
    }
  ]
}
```

**Use Cases:**

1.  **Progress Tracking:** Plotting a line graph of 1RM (One Rep Max) strength over time for a specific lift.
2.  **PR Detection:** Identifying personal records to highlight them in a custom dashboard.

-----