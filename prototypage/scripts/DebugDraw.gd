extends Node

@export var other_node: Node3D  # Node receiving raw phone rotations

# Calibration system
var _reference_quat := Quaternion.IDENTITY
var _is_calibrated := false

# Debug drawing system
var _debug_mesh := ImmediateMesh.new()
var _debug_mat := StandardMaterial3D.new()

func _ready():
	# Setup debug visualization
	var mesh_instance = MeshInstance3D.new()
	mesh_instance.mesh = _debug_mesh
	mesh_instance.material_override = _debug_mat
	add_child(mesh_instance)
	
	_debug_mat.vertex_color_use_as_albedo = true
	_debug_mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED

func _input(event):
	if event.is_action_pressed("ui_select"):  # Spacebar calibration
		_reference_quat = other_node.global_transform.basis.get_rotation_quaternion().inverse()
		_is_calibrated = true
		print("Calibration Complete - Reference Quaternion: ", _reference_quat)

func _process(_delta):
	# Clear previous frame's lines
	_debug_mesh.clear_surfaces()
	
	# Get current phone rotation
	var raw_quat = other_node.global_transform.basis.get_rotation_quaternion()
	
	# Apply calibration if needed
	var display_quat = raw_quat
	if _is_calibrated:
		display_quat = _reference_quat * raw_quat
	
	# Create basis for visualization
	var display_basis = Basis(display_quat)
	var position = other_node.global_position
	
	# Draw axes (persistent, no expiration)
	_debug_mesh.surface_begin(Mesh.PRIMITIVE_LINES)
	
	# Red: Right vector (X-axis)
	_debug_mesh.surface_set_color(Color.RED)
	_debug_mesh.surface_add_vertex(position)
	_debug_mesh.surface_add_vertex(position + display_basis.x * 0.5) 
	
	# Green: Up vector (Y-axis)
	_debug_mesh.surface_set_color(Color.GREEN)
	_debug_mesh.surface_add_vertex(position)
	_debug_mesh.surface_add_vertex(position + display_basis.y * 0.5) 
	
	# Blue: Forward vector (Z-axis)
	_debug_mesh.surface_set_color(Color.BLUE)
	_debug_mesh.surface_add_vertex(position)
	_debug_mesh.surface_add_vertex(position + display_basis.z * 0.5) 
	
	_debug_mesh.surface_end()
