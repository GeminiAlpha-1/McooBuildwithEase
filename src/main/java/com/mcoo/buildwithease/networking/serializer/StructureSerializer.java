package com.mcoo.buildwithease.networking.serializer;

import dev.huskuraft.universal.api.networking.NetByteBuf;
import dev.huskuraft.universal.api.networking.NetByteBufSerializer;
import com.mcoo.buildwithease.building.structure.BuildFeature;
import com.mcoo.buildwithease.building.structure.BuildFeatures;
import com.mcoo.buildwithease.building.structure.BuildMode;
import com.mcoo.buildwithease.building.structure.CircleStart;
import com.mcoo.buildwithease.building.structure.CubeFilling;
import com.mcoo.buildwithease.building.structure.CubeLength;
import com.mcoo.buildwithease.building.structure.LineDirection;
import com.mcoo.buildwithease.building.structure.PlaneFacing;
import com.mcoo.buildwithease.building.structure.PlaneFilling;
import com.mcoo.buildwithease.building.structure.PlaneLength;
import com.mcoo.buildwithease.building.structure.RaisedEdge;
import com.mcoo.buildwithease.building.structure.builder.Structure;
import com.mcoo.buildwithease.building.structure.builder.standard.Circle;
import com.mcoo.buildwithease.building.structure.builder.standard.Cone;
import com.mcoo.buildwithease.building.structure.builder.standard.Cuboid;
import com.mcoo.buildwithease.building.structure.builder.standard.Cylinder;
import com.mcoo.buildwithease.building.structure.builder.standard.DiagonalLine;
import com.mcoo.buildwithease.building.structure.builder.standard.DiagonalWall;
import com.mcoo.buildwithease.building.structure.builder.standard.Disable;
import com.mcoo.buildwithease.building.structure.builder.standard.Floor;
import com.mcoo.buildwithease.building.structure.builder.standard.Line;
import com.mcoo.buildwithease.building.structure.builder.standard.Pyramid;
import com.mcoo.buildwithease.building.structure.builder.standard.Single;
import com.mcoo.buildwithease.building.structure.builder.standard.SlopeFloor;
import com.mcoo.buildwithease.building.structure.builder.standard.Sphere;
import com.mcoo.buildwithease.building.structure.builder.standard.Wall;

public class StructureSerializer implements NetByteBufSerializer<Structure> {

    @Override
    public Structure read(NetByteBuf byteBuf) {
        return (switch (byteBuf.readEnum(BuildMode.class)) {
            case DISABLED -> new Disable();
            case SINGLE -> new Single();
            case LINE -> new Line();
            case WALL -> new Wall();
            case FLOOR -> new Floor();
            case CUBOID -> new Cuboid();
            case DIAGONAL_LINE -> new DiagonalLine();
            case DIAGONAL_WALL -> new DiagonalWall();
            case SLOPE_FLOOR -> new SlopeFloor();
            case CIRCLE -> new Circle();
            case CYLINDER -> new Cylinder();
            case SPHERE -> new Sphere();
            case PYRAMID -> new Pyramid();
            case CONE -> new Cone();
        }).withFeatures(byteBuf.readList(new BuildFeatureReader()));
    }

    @Override
    public void write(NetByteBuf byteBuf, Structure structure) {
        byteBuf.writeEnum(structure.getMode());
        byteBuf.writeList(structure.getFeatures(), new BuildFeatureReader());
    }

    public static class BuildFeatureReader implements NetByteBufSerializer<BuildFeature> {

        @Override
        public BuildFeature read(NetByteBuf byteBuf) {
            return switch (byteBuf.readEnum(BuildFeatures.class)) {
                case CIRCLE_START -> byteBuf.readEnum(CircleStart.class);
                case CUBE_FILLING -> byteBuf.readEnum(CubeFilling.class);
                case CUBE_LENGTH -> byteBuf.readEnum(CubeLength.class);
                case PLANE_FACING -> byteBuf.readEnum(PlaneFacing.class);
                case PLANE_FILLING -> byteBuf.readEnum(PlaneFilling.class);
                case PLANE_LENGTH -> byteBuf.readEnum(PlaneLength.class);
                case LINE_DIRECTION -> byteBuf.readEnum(LineDirection.class);
                case RAISED_EDGE -> byteBuf.readEnum(RaisedEdge.class);
            };
        }

        @Override
        public void write(NetByteBuf byteBuf, BuildFeature buildFeature) {
            byteBuf.writeEnum(buildFeature.getType());
            switch (buildFeature.getType()) {
                case CIRCLE_START -> byteBuf.writeEnum((CircleStart) buildFeature);
                case CUBE_FILLING -> byteBuf.writeEnum((CubeFilling) buildFeature);
                case CUBE_LENGTH -> byteBuf.writeEnum((CubeLength) buildFeature);
                case PLANE_FACING -> byteBuf.writeEnum((PlaneFacing) buildFeature);
                case PLANE_FILLING -> byteBuf.writeEnum((PlaneFilling) buildFeature);
                case PLANE_LENGTH -> byteBuf.writeEnum((PlaneLength) buildFeature);
                case LINE_DIRECTION -> byteBuf.writeEnum((LineDirection) buildFeature);
                case RAISED_EDGE -> byteBuf.writeEnum((RaisedEdge) buildFeature);
            }
        }
    }

}
