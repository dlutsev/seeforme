container_runtime := docker
project_name := see_for_me

$(info using ${container_runtime})

up: down
	${container_runtime}-compose -p ${project_name} up --build -d

down:
	${container_runtime}-compose -p ${project_name} down

clean:
	${container_runtime}-compose -p ${project_name} down -v

lint:
	make -C services lint

proto:
	make -C services protobuf
