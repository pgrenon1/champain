extends Area2D

var _id = 0

func _ready() -> void:
	var finish = RaceFinish.instance
	if !finish:
		push_error("No RaceFinish in scene (or RaceFinish is above this checkpoint)")
		return
	_id = finish.add_checkpoint()
	body_entered.connect(_entered)


func _physics_process(delta: float) -> void:
	
	pass
	
func _entered(body):
	if body is Player:
		body.validate_checkpoint(_id)
	
