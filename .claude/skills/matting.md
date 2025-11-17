# Image Matting Skill

This skill provides intelligent image matting (background removal) using ComfyUI's SAM (Segment Anything Model) workflow.

## Description

Performs automatic foreground extraction and background removal from images using advanced AI models.

## Input Parameters

The skill expects a JSON object with the following parameters:

```json
{
  "imagePath": "string (required) - Path to the input image file",
  "workflowName": "string (optional, default: sam_matting.json) - ComfyUI workflow to use",
  "threshold": "number (optional, default: 0.3) - SAM detection threshold (0.0-1.0)",
  "alphaMatting": "boolean (optional, default: true) - Enable edge refinement",
  "alphaMattingForegroundThreshold": "number (optional, default: 240) - Foreground threshold (200-255)",
  "alphaMattingBackgroundThreshold": "number (optional, default: 10) - Background threshold (0-50)",
  "alphaMattingErodeSize": "number (optional, default: 10) - Edge erosion size (0-20)"
}
```

## Output

Returns a JSON object containing:

```json
{
  "success": "boolean - Whether the operation succeeded",
  "outputFilename": "string - Name of the output file",
  "outputPath": "string - Full path to the output file",
  "outputUrl": "string - URL to access the output file",
  "promptId": "string - ComfyUI prompt ID",
  "executionTime": "number - Execution time in milliseconds",
  "errorMessage": "string (optional) - Error message if failed"
}
```

## Usage Example

### Input
```json
{
  "imagePath": "/tmp/input.jpg",
  "threshold": 0.3,
  "alphaMatting": true
}
```

### Output
```json
{
  "success": true,
  "outputFilename": "matting_result_12345.png",
  "outputPath": "/path/to/output/matting_result_12345.png",
  "outputUrl": "/output/matting_result_12345.png",
  "promptId": "abc-123-def",
  "executionTime": 5230
}
```

## Implementation

This skill uses the following workflow:

1. Upload image to ComfyUI server
2. Load the SAM matting workflow from `resources/workflows/sam_matting.json`
3. Configure workflow parameters based on input
4. Execute the workflow via ComfyUI API
5. Download and save the result image
6. Return the output file information

## Error Handling

Common errors:
- `Image file not found` - The specified image path doesn't exist
- `ComfyUI server not available` - Cannot connect to ComfyUI API
- `Workflow execution failed` - ComfyUI workflow encountered an error
- `Invalid parameters` - One or more parameters are out of valid range

## Dependencies

- ComfyUI server running on configured endpoint
- SAM model loaded in ComfyUI
- Required custom nodes installed in ComfyUI
