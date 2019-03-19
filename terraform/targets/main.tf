##############################################################################
# Providers
##############################################################################

provider "aws" {
  region  = "${var.aws_region}"
  profile = "${var.aws_profile}"
  version = "~> 1.0"
}

##############################################################################
# Resources
##############################################################################

resource "aws_alb_target_group" "shop_7080" {
  name     = "${var.environment}-${var.colour}-shop-7080"
  port     = 7080
  protocol = "HTTP"
  vpc_id   = "${data.terraform_remote_state.vpc.network-vpc-id}"

  health_check {
    protocol            = "HTTP"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200"
  }
}

resource "aws_alb_target_group" "shop_7443" {
  name     = "${var.environment}-${var.colour}-shop-7443"
  port     = 7443
  protocol = "HTTPS"
  vpc_id   = "${data.terraform_remote_state.vpc.network-vpc-id}"

  health_check {
    protocol            = "HTTPS"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    timeout             = 10
    interval            = 30
    matcher             = "200"
  }
}

resource aws_lb_target_group_attachment "shop_a_7080" {
  target_group_arn = "${aws_alb_target_group.shop_7080.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-a-id}"
  port             = "7080"
}

resource aws_lb_target_group_attachment "shop_b_7080" {
  target_group_arn = "${aws_alb_target_group.shop_7080.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-b-id}"
  port             = "7080"
}

resource aws_lb_target_group_attachment "shop_c_7080" {
  target_group_arn = "${aws_alb_target_group.shop_7080.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-c-id}"
  port             = "7080"
}

resource aws_lb_target_group_attachment "shop_a_7443" {
  target_group_arn = "${aws_alb_target_group.shop_7443.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-a-id}"
  port             = "7443"
}

resource aws_lb_target_group_attachment "shop_b_7443" {
  target_group_arn = "${aws_alb_target_group.shop_7443.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-b-id}"
  port             = "7443"
}

resource aws_lb_target_group_attachment "shop_c_7443" {
  target_group_arn = "${aws_alb_target_group.shop_7443.arn}"
  target_id        = "${data.terraform_remote_state.swarm.swarm-worker-c-id}"
  port             = "7443"
}

resource "aws_alb_listener_rule" "shop_7080" {
  listener_arn = "${data.terraform_remote_state.lb.lb-public-listener-http-arn}"
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.shop_7080.arn}"
  }

  condition {
    field  = "host-header"
    values = ["${var.environment}-${var.colour}-shop.${var.hosted_zone_name}"]
  }
}

resource "aws_alb_listener_rule" "shop_7443" {
  listener_arn = "${data.terraform_remote_state.lb.lb-public-listener-https-arn}"
  priority     = 200

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.shop_7443.arn}"
  }

  condition {
    field  = "host-header"
    values = ["${var.environment}-${var.colour}-shop.${var.hosted_zone_name}"]
  }
}

resource "aws_route53_record" "shop" {
  zone_id = "${var.hosted_zone_id}"
  name    = "${var.environment}-${var.colour}-shop.${var.hosted_zone_name}"
  type    = "A"

  alias {
    name                   = "${data.terraform_remote_state.lb.lb-public-alb-dns-name}"
    zone_id                = "${data.terraform_remote_state.lb.lb-public-alb-zone-id}"
    evaluate_target_health = true
  }
}
