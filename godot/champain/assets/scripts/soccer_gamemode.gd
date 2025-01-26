class_name SoccerGamemode extends Node

static var instance

@export var game_duration = 180.0

var _team_scores = [0, 0]
var _game_timer

@export var _ball : Ball
@export var _ball_spawn : Node2D
@export var _score : Label
@export var _timer : Label
@export var _announcer : Label


# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	instance = self
	_announcer.text = ""
	_game_timer = game_duration


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	_update_game_timer(delta)

func goal_scored(team_id: int):
	_team_scores[team_id] += 1
	_score.text = str(_team_scores[0]) + "            " + str(_team_scores[1])
	_announcer.text = "GOAL!"
	_ball.set_linear_velocity(Vector2())
	_ball.set_angular_velocity(0.0)
	_ball.global_translate(_ball_spawn.global_position - _ball.global_position)
	SpawnManager.instance.respawn_all()

func _update_game_timer(delta: float):
	_game_timer -= delta
	var minutes = _game_timer / 60
	var seconds = fmod(_game_timer, 60)
	_timer.text = "%01d:%02d" % [minutes, seconds]
