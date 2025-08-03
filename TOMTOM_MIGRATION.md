# Traffic Incident API Migration: MapQuest → TomTom

## Overview
This document describes the migration from MapQuest Traffic API to TomTom Traffic API for fetching traffic incidents in the BottleNex app.

## Changes Made

### 1. New Implementation
- **File**: `TomTomIncidentsFetcher.java`
- **API**: TomTom Traffic API v4
- **API Key**: `v0MHekYQmvieC8OOIosTag3grP8lSkmC`
- **Endpoint**: `https://api.tomtom.com/traffic/services/4/incidentDetails`

### 2. Updated Files
- **MainActivity.java**: Updated to use `TomTomIncidentsFetcher` instead of `MapQuestIncidentsFetcher`
- **Import statements**: Updated to import the new TomTom fetcher

### 3. Backup
- **MapQuestIncidentsFetcher.java.backup**: Backup of the original MapQuest implementation

## API Comparison

| Feature | MapQuest | TomTom |
|---------|----------|--------|
| Free Tier | Expensive | 2,500 requests/day |
| Coverage | Global | Global |
| Data Types | Accidents, Construction, Congestion | Accidents, Construction, Congestion, Road Hazards |
| Real-time | Yes | Yes |
| API Key Setup | Easy | Easy |

## TomTom API Features

### Incident Types Supported:
- **accident**: Traffic accidents
- **congestion**: Traffic congestion
- **construction**: Road construction
- **disabledvehicle**: Disabled vehicles
- **masstransit**: Mass transit issues
- **miscellaneous**: Other road incidents
- **othernews**: Road information
- **plannedevent**: Planned events
- **roadhazard**: Road hazards
- **weather**: Weather-related incidents

### API Parameters:
- **bbox**: Bounding box coordinates (minLat,minLon,maxLat,maxLon)
- **fields**: Specific data fields to retrieve
- **language**: Response language (en-GB)
- **timeValidityFilter**: Filter for present incidents only

## Implementation Details

### Same Interface
The new `TomTomIncidentsFetcher` maintains the same interface as the original `MapQuestIncidentsFetcher`:

```java
public static void fetchIncidents(double lat, double lon, double radiusKm, IncidentsCallback callback)
```

### Data Structure
The `Incident` class remains identical:
```java
public static class Incident {
    public final double lat;
    public final double lon;
    public final String type;
    public final String description;
    public final int distanceMeters;
}
```

### Coordinate Handling
- TomTom returns coordinates in [longitude, latitude] format
- The implementation correctly extracts and converts to [latitude, longitude]
- Distance calculation remains the same

## Benefits of Migration

1. **Cost Savings**: TomTom free tier (2,500 requests/day) vs MapQuest expensive pricing
2. **Better Coverage**: Enhanced incident types and descriptions
3. **Reliability**: TomTom is a well-established traffic data provider
4. **Future-Proof**: Better long-term sustainability

## Testing

To test the new implementation:
1. Build and run the app
2. Enable Road Incident Alerts in settings
3. Navigate to an area with known traffic incidents
4. Verify that incidents appear on the map
5. Check that proximity alerts work (within 800m)

## Monitoring

Monitor the following log tags for debugging:
- `TomTomIncidents`: API requests and responses
- `IncidentAlert`: Alert triggering logic

## Rollback Plan

If issues arise, you can quickly rollback by:
1. Restoring `MapQuestIncidentsFetcher.java` from backup
2. Updating `MainActivity.java` to use MapQuest again
3. Updating import statements

## API Key Management

The TomTom API key is currently hardcoded in `TomTomIncidentsFetcher.java`. For production, consider:
- Moving to a secure configuration file
- Using environment variables
- Implementing API key rotation

## Support

For TomTom API issues:
- Documentation: https://developer.tomtom.com/traffic-api
- Support: https://developer.tomtom.com/support
- API Status: https://status.tomtom.com/ 